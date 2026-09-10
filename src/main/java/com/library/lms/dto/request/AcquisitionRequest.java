package com.library.lms.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Logs new stock arriving for an existing book (e.g. a fresh order from a
 * supplier) and adds the quantity to that book's total/available copies.
 */
public record AcquisitionRequest(
        @NotNull(message = "bookId is required") Long bookId,
        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1") Integer quantity,
        String note
) {}
