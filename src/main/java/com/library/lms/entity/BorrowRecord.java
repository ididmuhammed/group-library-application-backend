package com.library.lms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "borrow_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BorrowRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User borrower;

    @Builder.Default
    @Column(nullable = false)
    private LocalDate borrowedDate = LocalDate.now();

    private LocalDate dueDate;

    private LocalDate returnedDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BorrowStatus status = BorrowStatus.BORROWED;

    // Notification bookkeeping — prevents the daily scan from re-sending the
    // same reminder/notice every day it runs.
    @Builder.Default
    @Column(nullable = false)
    private boolean dueReminderSent = false;

    private LocalDate lastOverdueNotifiedDate;

    public enum BorrowStatus {
        BORROWED, RETURNED, OVERDUE
    }
}
