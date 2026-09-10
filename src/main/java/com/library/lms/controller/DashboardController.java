package com.library.lms.controller;

import com.library.lms.dto.response.DashboardStatsResponse;
import com.library.lms.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Aggregated library statistics: book inventory, borrowing activity,
     * reservations, fines (overdue/lost/damaged) and user counts.
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public ResponseEntity<DashboardStatsResponse> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }
}
