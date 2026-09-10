package com.library.lms.repository;

import com.library.lms.entity.BorrowRecord;
import com.library.lms.entity.Fine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FineRepository extends JpaRepository<Fine, Long> {
    boolean existsByBorrowRecordAndReason(BorrowRecord borrowRecord, Fine.FineReason fineReason);

    // Fine.borrowRecord is LAZY, and open-in-view is disabled, so the plain
    // findAll()/findById() below leave a proxy that Jackson can't initialize
    // once the transaction/session has closed. Fetch it eagerly here instead
    // — book and borrower are already EAGER on BorrowRecord, so this one
    // join covers the whole chain the JSON response needs.
    @Query("SELECT f FROM Fine f JOIN FETCH f.borrowRecord br JOIN FETCH br.book JOIN FETCH br.borrower ORDER BY f.issuedDate DESC")
    List<Fine> findAllWithBorrowRecord();

    @Query("SELECT f FROM Fine f JOIN FETCH f.borrowRecord br JOIN FETCH br.book JOIN FETCH br.borrower WHERE f.id = :id")
    Optional<Fine> findByIdWithBorrowRecord(Long id);
}
