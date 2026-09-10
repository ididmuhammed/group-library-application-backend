package com.library.lms.service;

import com.library.lms.dto.response.DashboardStatsResponse;
import com.library.lms.entity.Book;
import com.library.lms.entity.BorrowRecord;
import com.library.lms.entity.Fine;
import com.library.lms.entity.Reservation;
import com.library.lms.entity.User;
import com.library.lms.repository.BookRepository;
import com.library.lms.repository.BorrowRecordRepository;
import com.library.lms.repository.FineRepository;
import com.library.lms.repository.ReservationRepository;
import com.library.lms.repository.UserRepository;
import com.library.lms.security.Permissions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Builds the aggregated statistics shown on the library dashboard: book
 * inventory, borrowing activity, reservations, fines (including damaged and
 * lost books) and user counts. Everything is derived from the current state
 * of the data on each call — simple, always-fresh, and fine at this system's
 * scale.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int DUE_SOON_DAYS = 3;
    private static final int TOP_BOOKS_LIMIT = 5;
    private static final int RECENT_LOANS_LIMIT = 8;

    private final BookRepository bookRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final FineRepository fineRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        List<Book> books = bookRepository.findAll();
        List<BorrowRecord> records = borrowRecordRepository.findAll();
        List<Fine> fines = fineRepository.findAll();
        List<Reservation> reservations = reservationRepository.findAll();
        List<User> users = userRepository.findAll();

        return new DashboardStatsResponse(
                LocalDateTime.now(),
                buildBookStats(books),
                buildBorrowStats(records),
                buildReservationStats(reservations),
                buildFineStats(fines),
                buildUserStats(users),
                buildTopBorrowedBooks(records),
                buildRecentLoans(records)
        );
    }

    private DashboardStatsResponse.BookStats buildBookStats(List<Book> books) {
        int totalCopies = books.stream().mapToInt(b -> nz(b.getTotalCopies())).sum();
        int availableCopies = books.stream().mapToInt(b -> nz(b.getAvailableCopies())).sum();
        long outOfStock = books.stream().filter(b -> nz(b.getAvailableCopies()) == 0).count();
        int lostCopies = books.stream().mapToInt(b -> nz(b.getLostCopies())).sum();
        int damagedCopies = books.stream().mapToInt(b -> nz(b.getDamagedCopies())).sum();

        Map<String, Long> byCategory = books.stream()
                .collect(Collectors.groupingBy(
                        b -> (b.getCategory() == null || b.getCategory().isBlank())
                                ? "Uncategorized" : b.getCategory(),
                        Collectors.counting()));

        List<DashboardStatsResponse.CategoryCount> categories = byCategory.entrySet().stream()
                .map(e -> new DashboardStatsResponse.CategoryCount(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(DashboardStatsResponse.CategoryCount::titleCount).reversed())
                .collect(Collectors.toList());

        return new DashboardStatsResponse.BookStats(
                books.size(), totalCopies, availableCopies, totalCopies - availableCopies, outOfStock, categories,
                lostCopies, damagedCopies);
    }

    private DashboardStatsResponse.BorrowStats buildBorrowStats(List<BorrowRecord> records) {
        LocalDate today = LocalDate.now();
        LocalDate dueSoonCutoff = today.plusDays(DUE_SOON_DAYS);

        long currentlyBorrowed = records.stream()
                .filter(r -> r.getStatus() == BorrowRecord.BorrowStatus.BORROWED)
                .count();

        long overdue = records.stream()
                .filter(r -> r.getStatus() == BorrowRecord.BorrowStatus.BORROWED)
                .filter(r -> r.getDueDate() != null && r.getDueDate().isBefore(today))
                .count();

        long dueSoon = records.stream()
                .filter(r -> r.getStatus() == BorrowRecord.BorrowStatus.BORROWED)
                .filter(r -> r.getDueDate() != null
                        && !r.getDueDate().isBefore(today)
                        && !r.getDueDate().isAfter(dueSoonCutoff))
                .count();

        long returned = records.stream()
                .filter(r -> r.getStatus() == BorrowRecord.BorrowStatus.RETURNED)
                .count();

        return new DashboardStatsResponse.BorrowStats(records.size(), currentlyBorrowed, overdue, dueSoon, returned);
    }

    private DashboardStatsResponse.ReservationStats buildReservationStats(List<Reservation> reservations) {
        Map<Reservation.ReservationStatus, Long> counts = reservations.stream()
                .collect(Collectors.groupingBy(Reservation::getStatus, Collectors.counting()));

        return new DashboardStatsResponse.ReservationStats(
                reservations.size(),
                counts.getOrDefault(Reservation.ReservationStatus.PENDING, 0L),
                counts.getOrDefault(Reservation.ReservationStatus.AVAILABLE, 0L),
                counts.getOrDefault(Reservation.ReservationStatus.FULFILLED, 0L),
                counts.getOrDefault(Reservation.ReservationStatus.CANCELLED, 0L),
                counts.getOrDefault(Reservation.ReservationStatus.EXPIRED, 0L));
    }

    private DashboardStatsResponse.FineStats buildFineStats(List<Fine> fines) {
        BigDecimal totalIssued = sum(fines);
        BigDecimal totalUnpaid = sum(filterByStatus(fines, Fine.FineStatus.UNPAID));
        BigDecimal totalPaid = sum(filterByStatus(fines, Fine.FineStatus.PAID));
        BigDecimal totalWaived = sum(filterByStatus(fines, Fine.FineStatus.WAIVED));

        long unpaidCount = fines.stream().filter(f -> f.getStatus() == Fine.FineStatus.UNPAID).count();
        long paidCount = fines.stream().filter(f -> f.getStatus() == Fine.FineStatus.PAID).count();
        long waivedCount = fines.stream().filter(f -> f.getStatus() == Fine.FineStatus.WAIVED).count();

        List<Fine> overdueFines = filterByReason(fines, Fine.FineReason.OVERDUE);
        List<Fine> lostFines = filterByReason(fines, Fine.FineReason.LOST_BOOK);
        List<Fine> damagedFines = filterByReason(fines, Fine.FineReason.DAMAGED_BOOK);

        return new DashboardStatsResponse.FineStats(
                totalIssued, totalUnpaid, totalPaid, totalWaived,
                unpaidCount, paidCount, waivedCount,
                overdueFines.size(), lostFines.size(), damagedFines.size(),
                sum(lostFines), sum(damagedFines));
    }

    private DashboardStatsResponse.UserStats buildUserStats(List<User> users) {
        long active = users.stream().filter(User::isEnabled).count();
        long admins = users.stream().filter(u -> hasRole(u, Permissions.ROLE_ADMIN)).count();
        long librarians = users.stream().filter(u -> hasRole(u, Permissions.ROLE_LIBRARIAN)).count();
        long members = users.stream().filter(u -> hasRole(u, Permissions.ROLE_MEMBER)).count();

        return new DashboardStatsResponse.UserStats(
                users.size(), active, users.size() - active, admins, librarians, members);
    }

    private List<DashboardStatsResponse.TopBookStat> buildTopBorrowedBooks(List<BorrowRecord> records) {
        Map<Long, Long> countsByBookId = records.stream()
                .collect(Collectors.groupingBy(r -> r.getBook().getId(), Collectors.counting()));

        Map<Long, Book> bookById = records.stream()
                .map(BorrowRecord::getBook)
                .collect(Collectors.toMap(Book::getId, b -> b, (a, b) -> a));

        return countsByBookId.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(TOP_BOOKS_LIMIT)
                .map(e -> {
                    Book book = bookById.get(e.getKey());
                    return new DashboardStatsResponse.TopBookStat(
                            book.getId(), book.getTitle(), book.getAuthor(), e.getValue());
                })
                .collect(Collectors.toList());
    }

    private List<DashboardStatsResponse.RecentLoan> buildRecentLoans(List<BorrowRecord> records) {
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;

        return records.stream()
                .sorted(Comparator.comparing(
                        BorrowRecord::getBorrowedDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(RECENT_LOANS_LIMIT)
                .map(r -> new DashboardStatsResponse.RecentLoan(
                        r.getId(),
                        r.getBook().getTitle(),
                        r.getBorrower().getFullName() != null
                                ? r.getBorrower().getFullName() : r.getBorrower().getUsername(),
                        r.getBorrowedDate() != null ? r.getBorrowedDate().format(fmt) : null,
                        r.getDueDate() != null ? r.getDueDate().format(fmt) : null,
                        r.getStatus().name(),
                        r.getStatus() == BorrowRecord.BorrowStatus.BORROWED
                                && r.getDueDate() != null && r.getDueDate().isBefore(today)))
                .collect(Collectors.toList());
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream().anyMatch(r -> r.getName().equals(roleName));
    }

    private List<Fine> filterByStatus(List<Fine> fines, Fine.FineStatus status) {
        return fines.stream().filter(f -> f.getStatus() == status).collect(Collectors.toList());
    }

    private List<Fine> filterByReason(List<Fine> fines, Fine.FineReason reason) {
        return fines.stream().filter(f -> f.getReason() == reason).collect(Collectors.toList());
    }

    private BigDecimal sum(List<Fine> fines) {
        return fines.stream()
                .map(Fine::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int nz(Integer value) {
        return value == null ? 0 : value;
    }
}
