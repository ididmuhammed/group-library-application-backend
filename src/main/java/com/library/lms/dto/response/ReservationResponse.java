package com.library.lms.dto.response;

import com.library.lms.entity.Reservation;

import java.time.LocalDate;

public record ReservationResponse(
        Long id,
        Long bookId,
        String bookTitle,
        Long userId,
        String username,
        LocalDate reservedDate,
        LocalDate notifiedDate,
        LocalDate pickupDeadline,
        LocalDate fulfilledDate,
        String status
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getBook().getId(),
                reservation.getBook().getTitle(),
                reservation.getUser().getId(),
                reservation.getUser().getUsername(),
                reservation.getReservedDate(),
                reservation.getNotifiedDate(),
                reservation.getPickupDeadline(),
                reservation.getFulfilledDate(),
                reservation.getStatus().name()
        );
    }
}

