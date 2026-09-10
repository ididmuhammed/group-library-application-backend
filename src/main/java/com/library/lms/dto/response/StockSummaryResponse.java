package com.library.lms.dto.response;

/**
 * Library-wide stock control snapshot: how many copies exist, how many are
 * on the shelf vs out on loan, and how many have been permanently removed
 * from circulation through loss or damage.
 */
public record StockSummaryResponse(
        long totalTitles,
        int totalCopies,
        int availableCopies,
        int borrowedCopies,
        int lostCopies,
        int damagedCopies,
        long outOfStockTitles
) {}
