package com.library.lms.service;

import com.library.lms.dto.request.BookRequest;
import com.library.lms.dto.response.BookResponse;
import com.library.lms.entity.Book;
import com.library.lms.entity.BorrowRecord;
import com.library.lms.entity.User;
import com.library.lms.exception.BadRequestException;
import com.library.lms.exception.ConflictException;
import com.library.lms.exception.ResourceNotFoundException;
import com.library.lms.repository.BookRepository;
import com.library.lms.repository.BorrowRecordRepository;
import com.library.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookService {

    private static final int DEFAULT_LOAN_DAYS = 1;

    private final BookRepository bookRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final UserRepository userRepository;
    private final FineService fineService;
    private final ReservationService reservationService;

    @Transactional
    public BookResponse createBook(BookRequest request) {
        if (request.isbn() != null && !request.isbn().isBlank() && bookRepository.existsByIsbn(request.isbn())) {
            throw new ConflictException("A book with ISBN " + request.isbn() + " already exists");
        }
        int copies = request.totalCopies() == null ? 1 : request.totalCopies();
        Book book = Book.builder()
                .title(request.title())
                .author(request.author())
                .isbn(request.isbn())
                .category(request.category())
                .totalCopies(copies)
                .availableCopies(copies)
                .build();
        return BookResponse.from(bookRepository.save(book));
    }

    @Transactional(readOnly = true)
    public List<BookResponse> getAllBooks() {
        return bookRepository.findAll().stream().map(BookResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BookResponse getBook(Long id) {
        return BookResponse.from(getBookEntity(id));
    }

    @Transactional
    public BookResponse updateBook(Long id, BookRequest request) {
        Book book = getBookEntity(id);
        int previousTotal = book.getTotalCopies();
        int newTotal = request.totalCopies() == null ? previousTotal : request.totalCopies();
        int delta = newTotal - previousTotal;

        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setIsbn(request.isbn());
        book.setCategory(request.category());
        book.setTotalCopies(newTotal);
        book.setAvailableCopies(Math.max(0, book.getAvailableCopies() + delta));

        return BookResponse.from(bookRepository.save(book));
    }

    @Transactional
    public void deleteBook(Long id) {
        Book book = getBookEntity(id);
        // Delete all borrow history and reservations associated with this book
        borrowRecordRepository.deleteByBook(book);
        reservationService.deleteReservationsForBook(book);
        // Now delete the book
        bookRepository.delete(book);
    }

    @Transactional
    public void borrowBook(Long bookId, String username) {
        Book book = getBookEntity(bookId);
        User user = getUserEntity(username);

        if (book.getAvailableCopies() <= 0) {
            throw new BadRequestException("No available copies of \"" + book.getTitle() + "\" right now");
        }

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        BorrowRecord record = BorrowRecord.builder()
                .book(book)
                .borrower(user)
                .borrowedDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(DEFAULT_LOAN_DAYS))
                .status(BorrowRecord.BorrowStatus.BORROWED)
                .build();
        borrowRecordRepository.save(record);
    }

    @Transactional
    public void returnBook(Long borrowRecordId, String username) {

        BorrowRecord record = borrowRecordRepository.findById(borrowRecordId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Borrow record not found: " + borrowRecordId
                        ));

        if (record.getStatus() == BorrowRecord.BorrowStatus.RETURNED) {
            throw new BadRequestException(
                    "This book has already been returned"
            );
        }

        record.setReturnedDate(LocalDate.now());
        record.setStatus(BorrowRecord.BorrowStatus.RETURNED);

        borrowRecordRepository.save(record);

        // Automatically create overdue fine if applicable
        fineService.createOverdueFine(record);

        Book book = record.getBook();

        book.setAvailableCopies(
                Math.min(
                        book.getTotalCopies(),
                        book.getAvailableCopies() + 1
                )
        );

        bookRepository.save(book);

        // If someone is waiting on this title, hand the freed copy straight
        // to them instead of leaving it open for anyone to borrow.
        reservationService.tryFulfillNextPending(book);
    }

    @Transactional(readOnly = true)
    public List<BorrowRecord> getMyBorrowedBooks(String username) {
        User user = getUserEntity(username);
        return borrowRecordRepository.findByBorrower(user);
    }

    private Book getBookEntity(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
    }

    private User getUserEntity(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
