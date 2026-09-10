package com.library.lms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Audit trail of every stock-affecting event for a book: new acquisitions,
 * copies lost, and copies damaged. Book.totalCopies/availableCopies/
 * lostCopies/damagedCopies hold the current running totals; this table is
 * the history of how they got there.
 */
@Entity
@Table(name = "inventory_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    // Set only when the entry originated from a specific loan (a borrowed
    // copy reported lost or damaged). Null for shelf adjustments and
    // acquisitions.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "borrow_record_id")
    private BorrowRecord borrowRecord;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChangeType type;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 500)
    private String note;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "performed_by_id")
    private User performedBy;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum ChangeType {
        ACQUISITION,
        LOST,
        DAMAGED
    }
}
