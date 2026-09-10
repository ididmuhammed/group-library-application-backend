package com.library.lms.dto.response;

import com.library.lms.entity.InventoryLog;

import java.time.LocalDateTime;

public record InventoryLogResponse(
        Long id,
        Long bookId,
        String bookTitle,
        String type,
        Integer quantity,
        String note,
        Long borrowRecordId,
        String performedBy,
        LocalDateTime createdAt
) {
    public static InventoryLogResponse from(InventoryLog log) {
        return new InventoryLogResponse(
                log.getId(),
                log.getBook().getId(),
                log.getBook().getTitle(),
                log.getType().name(),
                log.getQuantity(),
                log.getNote(),
                log.getBorrowRecord() != null ? log.getBorrowRecord().getId() : null,
                log.getPerformedBy() != null
                        ? (log.getPerformedBy().getFullName() != null
                                ? log.getPerformedBy().getFullName() : log.getPerformedBy().getUsername())
                        : "System",
                log.getCreatedAt()
        );
    }
}
