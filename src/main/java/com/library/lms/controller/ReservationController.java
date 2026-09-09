package com.library.lms.controller;


import com.library.lms.dto.response.BorrowRecordResponse;
import com.library.lms.dto.response.ReservationResponse;
import com.library.lms.security.UserPrincipal;
import com.library.lms.security.Permissions;
import com.library.lms.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('BOOK_RESERVE')")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ReservationResponse> reservations = reservationService.getMyReservations(principal.getUsername())
                .stream().map(ReservationResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(reservations);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('RESERVATION_READ_ALL')")
    public ResponseEntity<List<ReservationResponse>> getAllReservations() {
        List<ReservationResponse> reservations = reservationService.getAllReservations()
                .stream().map(ReservationResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(reservations);
    }

    @PostMapping("/{id}/pickup")
    @PreAuthorize("hasAuthority('BOOK_RESERVE')")
    public ResponseEntity<BorrowRecordResponse> pickup(@PathVariable Long id,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(BorrowRecordResponse.from(reservationService.pickup(id, principal.getUsername())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('BOOK_RESERVE')")
    public ResponseEntity<Void> cancel(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        boolean privileged = principal.getAuthorityNames().contains(Permissions.RESERVATION_READ_ALL);
        reservationService.cancelReservation(id, principal.getUsername(), privileged);
        return ResponseEntity.noContent().build();
    }
}

