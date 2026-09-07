package com.library.lms.repository;

import com.library.lms.entity.BorrowRecord;
import com.library.lms.entity.Fine;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FineRepository extends JpaRepository<Fine, Long> {
    boolean existsByBorrowRecordAndReason(BorrowRecord borrowRecord, Fine.FineReason fineReason);
}
