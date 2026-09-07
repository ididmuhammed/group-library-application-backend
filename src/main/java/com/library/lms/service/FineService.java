package com.library.lms.service;

import com.library.lms.entity.Book;
import com.library.lms.entity.BorrowRecord;
import com.library.lms.entity.Fine;
import com.library.lms.exception.BadRequestException;
import com.library.lms.exception.ResourceNotFoundException;
import com.library.lms.repository.BorrowRecordRepository;
import com.library.lms.repository.FineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FineService {

    private static final BigDecimal OVERDUE_RATE =
            BigDecimal.valueOf(1000);

    private static final BigDecimal LOST_DAMAGED_PENALTY =
            BigDecimal.valueOf(20_000);

    private final FineRepository fineRepository;
    private final BorrowRecordRepository borrowRecordRepository;


    /**
     * Automatically create an overdue fine.
     *
     * Fine = 1,000 UGX × number of overdue days
     */
    @Transactional
    public Fine createOverdueFine(BorrowRecord borrowRecord) {

        if (borrowRecord.getDueDate() == null) {
            throw new BadRequestException(
                    "Borrow record does not have a due date"
            );
        }

        LocalDate endDate = borrowRecord.getReturnedDate() != null
                ? borrowRecord.getReturnedDate()
                : LocalDate.now();

        long overdueDays = ChronoUnit.DAYS.between(
                borrowRecord.getDueDate(),
                endDate
        );

        if (overdueDays <= 0) {
            return null;
        }

        // Don't create duplicate overdue fines
        if (fineRepository.existsByBorrowRecordAndReason(
                borrowRecord,
                Fine.FineReason.OVERDUE
        )) {
            return null;
        }

        BigDecimal amount = OVERDUE_RATE.multiply(
                BigDecimal.valueOf(overdueDays)
        );

        Fine fine = Fine.builder()
                .borrowRecord(borrowRecord)
                .amount(amount)
                .reason(Fine.FineReason.OVERDUE)
                .status(Fine.FineStatus.UNPAID)
                .issuedDate(LocalDate.now())
                .build();

        return fineRepository.save(fine);
    }


    /**
     * Automatically create a lost-book fine.
     *
     * Fine = book copy price + 20,000 UGX
     */
    @Transactional
    public Fine createLostBookFine(BorrowRecord borrowRecord) {

        if (fineRepository.existsByBorrowRecordAndReason(
                borrowRecord,
                Fine.FineReason.LOST_BOOK
        )) {
            throw new BadRequestException(
                    "A lost-book fine already exists for this borrow record"
            );
        }

        Book book = borrowRecord.getBook();

        if (book.getBookCopyPrice() == null) {
            throw new BadRequestException(
                    "Book copy price has not been configured"
            );
        }

        BigDecimal amount = book.getBookCopyPrice()
                .add(LOST_DAMAGED_PENALTY);

        Fine fine = Fine.builder()
                .borrowRecord(borrowRecord)
                .amount(amount)
                .reason(Fine.FineReason.LOST_BOOK)
                .status(Fine.FineStatus.UNPAID)
                .issuedDate(LocalDate.now())
                .build();

        return fineRepository.save(fine);
    }


    /**
     * Automatically create a damaged-book fine.
     *
     * Fine = book copy price + 20,000 UGX
     */
    @Transactional
    public Fine createDamagedBookFine(BorrowRecord borrowRecord) {

        if (fineRepository.existsByBorrowRecordAndReason(
                borrowRecord,
                Fine.FineReason.DAMAGED_BOOK
        )) {
            throw new BadRequestException(
                    "A damaged-book fine already exists for this borrow record"
            );
        }

        Book book = borrowRecord.getBook();

        if (book.getBookCopyPrice() == null) {
            throw new BadRequestException(
                    "Book copy price has not been configured"
            );
        }

        BigDecimal amount = book.getBookCopyPrice()
                .add(LOST_DAMAGED_PENALTY);

        Fine fine = Fine.builder()
                .borrowRecord(borrowRecord)
                .amount(amount)
                .reason(Fine.FineReason.DAMAGED_BOOK)
                .status(Fine.FineStatus.UNPAID)
                .issuedDate(LocalDate.now())
                .build();

        return fineRepository.save(fine);
    }


    /**
     * Get all fines.
     */
    @Transactional(readOnly = true)
    public List<Fine> getAllFines() {
        return fineRepository.findAll();
    }


    /**
     * Get a fine by ID.
     */
    @Transactional(readOnly = true)
    public Fine getFine(Long id) {

        return fineRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Fine not found with id: " + id
                        )
                );
    }


    /**
     * Pay a fine.
     */
    @Transactional
    public Fine payFine(Long fineId) {

        Fine fine = getFine(fineId);

        if (fine.getStatus() == Fine.FineStatus.PAID) {
            throw new BadRequestException(
                    "This fine has already been paid"
            );
        }

        if (fine.getStatus() == Fine.FineStatus.WAIVED) {
            throw new BadRequestException(
                    "A waived fine cannot be paid"
            );
        }

        fine.setStatus(Fine.FineStatus.PAID);
        fine.setPaidDate(LocalDate.now());

        return fineRepository.save(fine);
    }


    /**
     * Waive a fine.
     */
    @Transactional
    public Fine waiveFine(Long fineId) {

        Fine fine = getFine(fineId);

        if (fine.getStatus() == Fine.FineStatus.PAID) {
            throw new BadRequestException(
                    "A paid fine cannot be waived"
            );
        }

        fine.setStatus(Fine.FineStatus.WAIVED);
        fine.setPaidDate(null);

        return fineRepository.save(fine);
    }
}