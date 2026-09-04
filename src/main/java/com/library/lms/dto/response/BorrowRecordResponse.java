package com.library.lms.dto.response;

import com.library.lms.entity.BorrowRecord;

import java.time.LocalDate;

public record BorrowRecordResponse(
        Long id,
        Long bookId,
        String bookTitle,
        LocalDate borrowedDate,
        LocalDate dueDate,
        LocalDate returnedDate,
        String status
) {
    public static BorrowRecordResponse from(BorrowRecord record) {
        return new BorrowRecordResponse(
                record.getId(),
                record.getBook().getId(),
                record.getBook().getTitle(),
                record.getBorrowedDate(),
                record.getDueDate(),
                record.getReturnedDate(),
                record.getStatus().name()
        );
    }
}
