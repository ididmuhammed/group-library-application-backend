package com.library.lms.service;

import com.library.lms.entity.Book;
import com.library.lms.entity.BorrowRecord;
import com.library.lms.entity.Reservation;
import com.library.lms.entity.Reservation.ReservationStatus;
import com.library.lms.entity.User;
import com.library.lms.exception.BadRequestException;
import com.library.lms.exception.ConflictException;
import com.library.lms.exception.ResourceNotFoundException;
import com.library.lms.repository.BookRepository;
import com.library.lms.repository.BorrowRecordRepository;
import com.library.lms.repository.ReservationRepository;
import com.library.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Owns the hold/reservation lifecycle: PENDING -> AVAILABLE -> FULFILLED,
 * or PENDING/AVAILABLE -> CANCELLED / EXPIRED.
 *
 * A reservation only ever "holds" a physical copy while it is AVAILABLE:
 * the moment a copy is set aside for a member, it is subtracted from
 * Book.availableCopies so it can't be borrowed out from under them, and it's
 * added back the moment the hold is cancelled, expires, or is picked up.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final int PICKUP_WINDOW_DAYS = 3;
    private static final int DEFAULT_LOAN_DAYS = 1; // mirrors BookService

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final EmailService emailService;

    @Transactional
    public Reservation reserveBook(Long bookId, String username) {
        Book book = getBookEntity(bookId);
        User user = getUserEntity(username);

        if (reservationRepository.existsByBookAndUserAndStatusIn(
                book, user, List.of(ReservationStatus.PENDING, ReservationStatus.AVAILABLE))) {
            throw new ConflictException(
                    "You already have an active reservation for \"" + book.getTitle() + "\"");
        }

        if (book.getAvailableCopies() > 0) {
            throw new BadRequestException(
                    "\"" + book.getTitle() + "\" is available right now — borrow it instead of reserving it");
        }

        Reservation reservation = Reservation.builder()
                .book(book)
                .user(user)
                .reservedDate(LocalDate.now())
                .status(ReservationStatus.PENDING)
                .build();

        return reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public List<Reservation> getMyReservations(String username) {
        return reservationRepository.findByUser(getUserEntity(username));
    }

    @Transactional(readOnly = true)
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    /**
     * Cancel a reservation. Members may only cancel their own; staff (callers
     * passing privileged=true) may cancel any.
     */
    @Transactional
    public void cancelReservation(Long reservationId, String username, boolean privileged) {
        Reservation reservation = getReservationEntity(reservationId);

        if (!privileged && !reservation.getUser().getUsername().equals(username)) {
            throw new AccessDeniedException("You do not have permission to cancel this reservation");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED
                || reservation.getStatus() == ReservationStatus.FULFILLED
                || reservation.getStatus() == ReservationStatus.EXPIRED) {
            throw new BadRequestException("This reservation is no longer active");
        }

        boolean wasHoldingACopy = reservation.getStatus() == ReservationStatus.AVAILABLE;
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        if (wasHoldingACopy) {
            releaseCopyAndPromoteNext(reservation.getBook());
        }
    }

    /**
     * Convert an AVAILABLE reservation into an actual loan. Only the member
     * who holds the reservation may pick it up.
     */
    @Transactional
    public BorrowRecord pickup(Long reservationId, String username) {
        Reservation reservation = getReservationEntity(reservationId);

        if (!reservation.getUser().getUsername().equals(username)) {
            throw new AccessDeniedException("You do not have permission to pick up this reservation");
        }

        if (reservation.getStatus() != ReservationStatus.AVAILABLE) {
            throw new BadRequestException("This reservation isn't ready for pickup");
        }

        reservation.setStatus(ReservationStatus.FULFILLED);
        reservation.setFulfilledDate(LocalDate.now());
        reservationRepository.save(reservation);

        // The copy was already held aside (subtracted from availableCopies)
        // when the reservation became AVAILABLE, so we don't touch the
        // book's copy count here — just record the loan.
        BorrowRecord record = BorrowRecord.builder()
                .book(reservation.getBook())
                .borrower(reservation.getUser())
                .borrowedDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(DEFAULT_LOAN_DAYS))
                .status(BorrowRecord.BorrowStatus.BORROWED)
                .build();

        return borrowRecordRepository.save(record);
    }

    /**
     * Called whenever a copy of a book becomes free (a return, a cancelled
     * hold, or an expired hold). If someone is waiting, the copy is set
     * aside for the longest-waiting PENDING reservation and they're
     * emailed; otherwise the copy is left available for direct borrowing.
     */
    @Transactional
    public void tryFulfillNextPending(Book book) {
        reservationRepository.findFirstByBookAndStatusOrderByReservedDateAsc(book, ReservationStatus.PENDING)
                .ifPresent(next -> {
                    if (book.getAvailableCopies() <= 0) {
                        return; // nothing to hand out
                    }

                    book.setAvailableCopies(book.getAvailableCopies() - 1);
                    bookRepository.save(book);

                    next.setStatus(ReservationStatus.AVAILABLE);
                    next.setNotifiedDate(LocalDate.now());
                    next.setPickupDeadline(LocalDate.now().plusDays(PICKUP_WINDOW_DAYS));
                    reservationRepository.save(next);

                    try {
                        emailService.sendReservationAvailableEmail(
                                next.getUser().getEmail(),
                                next.getUser().getFullName(),
                                book.getTitle(),
                                next.getPickupDeadline());
                    } catch (Exception e) {
                        log.error("Failed to send reservation-available email for reservation {}",
                                next.getId(), e);
                    }
                });
    }

    /**
     * Releases a copy that was being held for a reservation back onto the
     * shelf, then immediately offers it to the next person in line (if any).
     */
    private void releaseCopyAndPromoteNext(Book book) {
        book.setAvailableCopies(Math.min(book.getTotalCopies(), book.getAvailableCopies() + 1));
        bookRepository.save(book);
        tryFulfillNextPending(book);
    }

    /**
     * Scheduled entry point: expires any AVAILABLE reservation whose pickup
     * window has passed, releasing the held copy back to the shelf (and on
     * to the next person waiting, if there is one).
     */
    @Transactional
    public int expireStalePickups() {
        List<Reservation> stale = reservationRepository
                .findByStatusAndPickupDeadlineBefore(ReservationStatus.AVAILABLE, LocalDate.now());

        for (Reservation reservation : stale) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);
            releaseCopyAndPromoteNext(reservation.getBook());
        }

        return stale.size();
    }

    @Transactional
    public void deleteReservationsForBook(Book book) {
        reservationRepository.deleteByBook(book);
    }

    private Book getBookEntity(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
    }

    private User getUserEntity(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private Reservation getReservationEntity(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
    }
}
