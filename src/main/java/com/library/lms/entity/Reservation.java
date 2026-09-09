package com.library.lms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * A hold placed by a member on a book that has no copies available right now.
 * Lifecycle: PENDING -> AVAILABLE (a copy was set aside, pickup window started)
 *                    -> FULFILLED (member borrowed the held copy)
 *                    -> EXPIRED   (pickup window passed, copy released back to the shelf)
 *            PENDING/AVAILABLE -> CANCELLED (member or staff cancelled it)
 */
@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @Column(nullable = false)
    private LocalDate reservedDate = LocalDate.now();

    // Set when a copy is set aside for this reservation (status -> AVAILABLE).
    private LocalDate notifiedDate;

    // Member must pick the book up by this date, or the hold is released.
    private LocalDate pickupDeadline;

    private LocalDate fulfilledDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.PENDING;

    public enum ReservationStatus {
        PENDING, AVAILABLE, FULFILLED, CANCELLED, EXPIRED
    }
}
