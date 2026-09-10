package com.library.lms.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Reports copies found lost or damaged on the shelf — i.e. not currently
 * tied to an active loan (damaged in storage, missing after a stock take,
 * etc). Removes the quantity from that book's available/total copies.
 */
public record ShelfAdjustmentRequest(
        @NotNull(message = "bookId is required") Long bookId,
        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1") Integer quantity,
        String note
) {}
