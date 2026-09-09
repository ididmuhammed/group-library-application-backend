package com.library.lms.repository;

import com.library.lms.entity.Book;
import com.library.lms.entity.BorrowRecord;
import com.library.lms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {
    List<BorrowRecord> findByBorrower(User borrower);
    List<BorrowRecord> findByBorrowerAndStatus(User borrower, BorrowRecord.BorrowStatus status);
    void deleteByBook(Book book);

    // Loans due on a given date that haven't had their reminder sent yet.
    List<BorrowRecord> findByStatusAndDueDateAndDueReminderSentFalse(
            BorrowRecord.BorrowStatus status, LocalDate dueDate);

    // Loans that are currently overdue (still borrowed, due date already passed).
    List<BorrowRecord> findByStatusAndDueDateBefore(
            BorrowRecord.BorrowStatus status, LocalDate date);
}
