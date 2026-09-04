package com.library.lms.repository;

import com.library.lms.entity.BorrowRecord;
import com.library.lms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {
    List<BorrowRecord> findByBorrower(User borrower);
    List<BorrowRecord> findByBorrowerAndStatus(User borrower, BorrowRecord.BorrowStatus status);
}
