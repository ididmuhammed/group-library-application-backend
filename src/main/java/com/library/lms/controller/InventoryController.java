package com.library.lms.controller;

import com.library.lms.dto.request.AcquisitionRequest;
import com.library.lms.dto.request.InventoryNoteRequest;
import com.library.lms.dto.request.ShelfAdjustmentRequest;
import com.library.lms.dto.response.InventoryLogResponse;
import com.library.lms.dto.response.StockSummaryResponse;
import com.library.lms.security.UserPrincipal;
import com.library.lms.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Inventory and stock control for admins/librarians: monitoring total
 * stock, logging new acquisitions, and writing off copies that turn out
 * lost or damaged (either while on loan or discovered on the shelf).
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public ResponseEntity<StockSummaryResponse> getStockSummary() {
        return ResponseEntity.ok(inventoryService.getStockSummary());
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public ResponseEntity<List<InventoryLogResponse>> getLogs() {
        return ResponseEntity.ok(inventoryService.getAllLogs());
    }

    @PostMapping("/acquisitions")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE')")
    public ResponseEntity<InventoryLogResponse> recordAcquisition(
            @Valid @RequestBody AcquisitionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventoryService.recordAcquisition(request, principal.getUsername()));
    }

    @PostMapping("/borrow-records/{recordId}/lost")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE')")
    public ResponseEntity<InventoryLogResponse> markLoanLost(
            @PathVariable Long recordId,
            @RequestBody(required = false) InventoryNoteRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        String note = request == null ? null : request.note();
        return ResponseEntity.ok(inventoryService.markLoanLost(recordId, note, principal.getUsername()));
    }

    @PostMapping("/borrow-records/{recordId}/damaged")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE')")
    public ResponseEntity<InventoryLogResponse> markLoanDamaged(
            @PathVariable Long recordId,
            @RequestBody(required = false) InventoryNoteRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        String note = request == null ? null : request.note();
        return ResponseEntity.ok(inventoryService.markLoanDamaged(recordId, note, principal.getUsername()));
    }

    @PostMapping("/shelf-loss")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE')")
    public ResponseEntity<InventoryLogResponse> recordShelfLoss(
            @Valid @RequestBody ShelfAdjustmentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventoryService.recordShelfLoss(request, principal.getUsername()));
    }

    @PostMapping("/shelf-damage")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE')")
    public ResponseEntity<InventoryLogResponse> recordShelfDamage(
            @Valid @RequestBody ShelfAdjustmentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventoryService.recordShelfDamage(request, principal.getUsername()));
    }
}
