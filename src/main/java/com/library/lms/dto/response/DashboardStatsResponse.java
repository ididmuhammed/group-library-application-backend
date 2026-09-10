package com.library.lms.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Aggregated snapshot of library activity, powering the admin/librarian
 * dashboard. Everything here is computed on demand from current data —
 * nothing is persisted or cached.
 */
public record DashboardStatsResponse(
        LocalDateTime generatedAt,
        BookStats books,
        BorrowStats borrowing,
        ReservationStats reservations,
        FineStats fines,
        UserStats users,
        List<TopBookStat> topBorrowedBooks,
        List<RecentLoan> recentLoans
) {

    public record BookStats(
            long totalTitles,
            int totalCopies,
            int availableCopies,
            int borrowedCopies,
            long outOfStockTitles,
            List<CategoryCount> byCategory,
            int lostCopies,
            int damagedCopies
    ) {}

    public record CategoryCount(String category, long titleCount) {}

    public record BorrowStats(
            long totalRecords,
            long currentlyBorrowed,
            long overdue,
            long dueSoon,
            long returned
    ) {}

    public record ReservationStats(
            long total,
            long pending,
            long available,
            long fulfilled,
            long cancelled,
            long expired
    ) {}

    public record FineStats(
            BigDecimal totalIssued,
            BigDecimal totalUnpaid,
            BigDecimal totalPaid,
            BigDecimal totalWaived,
            long unpaidCount,
            long paidCount,
            long waivedCount,
            long overdueFineCount,
            long lostBookCount,
            long damagedBookCount,
            BigDecimal lostBookAmount,
            BigDecimal damagedBookAmount
    ) {}

    public record UserStats(
            long totalUsers,
            long activeUsers,
            long disabledUsers,
            long admins,
            long librarians,
            long members
    ) {}

    public record TopBookStat(Long bookId, String title, String author, long borrowCount) {}

    public record RecentLoan(
            Long id,
            String bookTitle,
            String borrowerName,
            String borrowedDate,
            String dueDate,
            String status,
            boolean overdue
    ) {}
}
