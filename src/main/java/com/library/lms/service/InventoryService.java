package com.library.lms.service;

import com.library.lms.dto.request.AcquisitionRequest;
import com.library.lms.dto.request.ShelfAdjustmentRequest;
import com.library.lms.dto.response.InventoryLogResponse;
import com.library.lms.dto.response.StockSummaryResponse;
import com.library.lms.entity.Book;
import com.library.lms.entity.BorrowRecord;
import com.library.lms.entity.InventoryLog;
import com.library.lms.entity.User;
import com.library.lms.exception.BadRequestException;
import com.library.lms.exception.ResourceNotFoundException;
import com.library.lms.repository.BookRepository;
import com.library.lms.repository.BorrowRecordRepository;
import com.library.lms.repository.InventoryLogRepository;
import com.library.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Stock control: logging new acquisitions and writing off copies that turn
 * out lost or damaged, either while checked out or found on the shelf.
 * Book.totalCopies/availableCopies/lostCopies/damagedCopies are kept as the
 * current running totals; every change is also appended to InventoryLog so
 * admins/librarians can audit how stock levels got where they are.
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final BookRepository bookRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final UserRepository userRepository;
    private final FineService fineService;

    @Transactional
    public InventoryLogResponse recordAcquisition(AcquisitionRequest request, String username) {
        Book book = getBook(request.bookId());
        int quantity = request.quantity();

        book.setTotalCopies(book.getTotalCopies() + quantity);
        book.setAvailableCopies(book.getAvailableCopies() + quantity);
        bookRepository.save(book);

        InventoryLog log = saveLog(book, null, InventoryLog.ChangeType.ACQUISITION,
                quantity, request.note(), username);
        return InventoryLogResponse.from(log);
    }

    /** A specific borrowed copy is confirmed lost — writes it off and issues the lost-book fine. */
    @Transactional
    public InventoryLogResponse markLoanLost(Long borrowRecordId, String note, String username) {
        BorrowRecord record = getActiveBorrowRecord(borrowRecordId);

        record.setStatus(BorrowRecord.BorrowStatus.LOST);
        record.setReturnedDate(LocalDate.now());
        borrowRecordRepository.save(record);

        Book book = record.getBook();
        book.setTotalCopies(Math.max(0, book.getTotalCopies() - 1));
        book.setLostCopies(book.getLostCopies() + 1);
        bookRepository.save(book);

        fineService.createLostBookFine(record);

        InventoryLog log = saveLog(book, record, InventoryLog.ChangeType.LOST, 1, note, username);
        return InventoryLogResponse.from(log);
    }

    /** A specific borrowed copy comes back damaged beyond return-to-shelf condition. */
    @Transactional
    public InventoryLogResponse markLoanDamaged(Long borrowRecordId, String note, String username) {
        BorrowRecord record = getActiveBorrowRecord(borrowRecordId);

        record.setStatus(BorrowRecord.BorrowStatus.DAMAGED);
        record.setReturnedDate(LocalDate.now());
        borrowRecordRepository.save(record);

        Book book = record.getBook();
        book.setTotalCopies(Math.max(0, book.getTotalCopies() - 1));
        book.setDamagedCopies(book.getDamagedCopies() + 1);
        bookRepository.save(book);

        fineService.createDamagedBookFine(record);

        InventoryLog log = saveLog(book, record, InventoryLog.ChangeType.DAMAGED, 1, note, username);
        return InventoryLogResponse.from(log);
    }

    /** Copies discovered lost on the shelf (not currently out on loan) — e.g. after a stock take. */
    @Transactional
    public InventoryLogResponse recordShelfLoss(ShelfAdjustmentRequest request, String username) {
        return recordShelfAdjustment(request, InventoryLog.ChangeType.LOST, username);
    }

    /** Copies discovered damaged on the shelf (not currently out on loan). */
    @Transactional
    public InventoryLogResponse recordShelfDamage(ShelfAdjustmentRequest request, String username) {
        return recordShelfAdjustment(request, InventoryLog.ChangeType.DAMAGED, username);
    }

    private InventoryLogResponse recordShelfAdjustment(
            ShelfAdjustmentRequest request, InventoryLog.ChangeType type, String username) {
        Book book = getBook(request.bookId());
        int quantity = request.quantity();

        if (quantity > book.getAvailableCopies()) {
            throw new BadRequestException(
                    "Only " + book.getAvailableCopies() + " copies of \"" + book.getTitle()
                            + "\" are currently on the shelf");
        }

        book.setAvailableCopies(book.getAvailableCopies() - quantity);
        book.setTotalCopies(Math.max(0, book.getTotalCopies() - quantity));

        if (type == InventoryLog.ChangeType.LOST) {
            book.setLostCopies(book.getLostCopies() + quantity);
        } else {
            book.setDamagedCopies(book.getDamagedCopies() + quantity);
        }
        bookRepository.save(book);

        InventoryLog log = saveLog(book, null, type, quantity, request.note(), username);
        return InventoryLogResponse.from(log);
    }

    @Transactional(readOnly = true)
    public List<InventoryLogResponse> getAllLogs() {
        return inventoryLogRepository.findAllWithDetails().stream()
                .map(InventoryLogResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StockSummaryResponse getStockSummary() {
        List<Book> books = bookRepository.findAll();

        long totalTitles = books.size();
        int totalCopies = books.stream().mapToInt(b -> nz(b.getTotalCopies())).sum();
        int availableCopies = books.stream().mapToInt(b -> nz(b.getAvailableCopies())).sum();
        int lostCopies = books.stream().mapToInt(b -> nz(b.getLostCopies())).sum();
        int damagedCopies = books.stream().mapToInt(b -> nz(b.getDamagedCopies())).sum();
        int borrowedCopies = totalCopies - availableCopies;
        long outOfStockTitles = books.stream().filter(b -> nz(b.getAvailableCopies()) == 0).count();

        return new StockSummaryResponse(
                totalTitles, totalCopies, availableCopies, borrowedCopies,
                lostCopies, damagedCopies, outOfStockTitles);
    }

    private BorrowRecord getActiveBorrowRecord(Long borrowRecordId) {
        BorrowRecord record = borrowRecordRepository.findById(borrowRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrow record not found: " + borrowRecordId));

        if (record.getStatus() != BorrowRecord.BorrowStatus.BORROWED
                && record.getStatus() != BorrowRecord.BorrowStatus.OVERDUE) {
            throw new BadRequestException(
                    "This loan is already marked " + record.getStatus().name().toLowerCase()
                            + " and cannot be updated again");
        }
        return record;
    }

    private InventoryLog saveLog(Book book, BorrowRecord record, InventoryLog.ChangeType type,
                                  int quantity, String note, String username) {
        User performedBy = username == null ? null : userRepository.findByUsername(username).orElse(null);

        InventoryLog log = InventoryLog.builder()
                .book(book)
                .borrowRecord(record)
                .type(type)
                .quantity(quantity)
                .note(note)
                .performedBy(performedBy)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        return inventoryLogRepository.save(log);
    }

    private Book getBook(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
    }

    private int nz(Integer value) {
        return value == null ? 0 : value;
    }
}
