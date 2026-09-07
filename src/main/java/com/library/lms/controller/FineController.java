package com.library.lms.controller;

import com.library.lms.entity.Fine;
import com.library.lms.service.FineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/fines")
@RequiredArgsConstructor
public class FineController {

    private final FineService fineService;

    /**
     * Get all fines
     */
    @GetMapping()
    public ResponseEntity<List<Fine>> getAllFines() {
        return ResponseEntity.ok(fineService.getAllFines());
    }
    /**
     * Get a single fine
     */
    @GetMapping("{id}")
    public ResponseEntity<Fine> getFine(@PathVariable Long id) {
        return ResponseEntity.ok(fineService.getFine(id));
    }

    /**
     * Pay a fine
     */
    @PatchMapping("{id}/pay")
    public ResponseEntity<Fine> payFine(@PathVariable Long id) {
        return ResponseEntity.ok(fineService.payFine(id));
    }

    /**
     * Waive a fine
     */
    @PatchMapping("fines/{id}/waive")
    public ResponseEntity<Fine> waiveFine(@PathVariable Long id) {
        return ResponseEntity.ok(fineService.waiveFine(id));
    }
}