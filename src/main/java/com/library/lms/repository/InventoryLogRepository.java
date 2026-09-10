package com.library.lms.repository;

import com.library.lms.entity.Book;
import com.library.lms.entity.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {

    // book/borrowRecord/performedBy are all EAGER, so plain findAll() would
    // still N+1; fetch-join them here the same way FineRepository does.
    @Query("SELECT l FROM InventoryLog l " +
            "LEFT JOIN FETCH l.book " +
            "LEFT JOIN FETCH l.borrowRecord br " +
            "LEFT JOIN FETCH l.performedBy " +
            "ORDER BY l.createdAt DESC")
    List<InventoryLog> findAllWithDetails();

    List<InventoryLog> findByBookOrderByCreatedAtDesc(Book book);
}
