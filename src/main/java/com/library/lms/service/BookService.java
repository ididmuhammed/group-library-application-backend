package com.library.lms.service;

import com.library.lms.dto.request.BookRequest;
import com.library.lms.dto.response.BookResponse;
import com.library.lms.dto.response.PageResponse;
import com.library.lms.entity.Book;
import com.library.lms.entity.BorrowRecord;
import com.library.lms.entity.User;
import com.library.lms.exception.BadRequestException;
import com.library.lms.exception.ConflictException;
import com.library.lms.exception.ResourceNotFoundException;
import com.library.lms.repository.BookRepository;
import com.library.lms.repository.BorrowRecordRepository;
import com.library.lms.repository.UserRepository;
import com.library.lms.specification.BookSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookService {

    private static final int DEFAULT_LOAN_DAYS = 1;
    private static final String BOOK_IMAGE_FOLDER = "books";

    private final BookRepository bookRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final UserRepository userRepository;
    private final FineService fineService;
    private final ReservationService reservationService;
    private final ImageService imageService;

    @Transactional
    public BookResponse createBook(BookRequest request, MultipartFile image) {
        if (request.isbn() != null && !request.isbn().isBlank() && bookRepository.existsByIsbn(request.isbn())) {
            throw new ConflictException("A book with ISBN " + request.isbn() + " already exists");
        }
        int copies = request.totalCopies() == null ? 1 : request.totalCopies();
        Book.BookBuilder bookBuilder = Book.builder()
                .title(request.title())
                .author(request.author())
                .isbn(request.isbn())
                .category(request.category())
                .totalCopies(copies)
                .availableCopies(copies);

        if (image != null && !image.isEmpty()) {
            var uploaded = imageService.upload(image, BOOK_IMAGE_FOLDER);
            bookBuilder.imageUrl(uploaded.url()).imagePublicId(uploaded.publicId());
        }

        return BookResponse.from(bookRepository.save(bookBuilder.build()));
    }

    /**
     * Paged, sorted, filtered catalog listing.
     *
     * @param search       free-text match against title/author/isbn/category
     * @param category     exact category filter
     * @param availableOnly when true, only books with copies currently available
     * @param pageable     page/size/sort - sortable properties: id, title, author,
     *                     isbn, category, totalCopies, availableCopies, lostCopies, damagedCopies
     */
    @Transactional(readOnly = true)
    public PageResponse<BookResponse> getAllBooks(String search, String category, Boolean availableOnly, Pageable pageable) {
        Page<BookResponse> page = bookRepository
                .findAll(BookSpecifications.withFilters(search, category, availableOnly), pageable)
                .map(BookResponse::from);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public BookResponse getBook(Long id) {
        return BookResponse.from(getBookEntity(id));
    }

    @Transactional
    public BookResponse updateBook(Long id, BookRequest request, MultipartFile image) {
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

        String oldPublicId = null;
        if (image != null && !image.isEmpty()) {
            oldPublicId = book.getImagePublicId();
            var uploaded = imageService.upload(image, BOOK_IMAGE_FOLDER);
            book.setImageUrl(uploaded.url());
            book.setImagePublicId(uploaded.publicId());
        }

        Book saved = bookRepository.save(book);
        // Only remove the old asset once the new one is safely persisted
        imageService.delete(oldPublicId);

        return BookResponse.from(saved);
    }

    @Transactional
    public void deleteBook(Long id) {
        Book book = getBookEntity(id);
        String publicId = book.getImagePublicId();
        // Delete all borrow history and reservations associated with this book
        borrowRecordRepository.deleteByBook(book);
        reservationService.deleteReservationsForBook(book);
        // Now delete the book
        bookRepository.delete(book);
        imageService.delete(publicId);
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
