package com.library.lms.dto.response;

import com.library.lms.entity.Book;

public record BookResponse(
        Long id,
        String title,
        String author,
        String isbn,
        String category,
        Integer totalCopies,
        Integer availableCopies,
        Integer lostCopies,
        Integer damagedCopies,
        String imageUrl
) {
    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getId(), book.getTitle(), book.getAuthor(), book.getIsbn(),
                book.getCategory(), book.getTotalCopies(), book.getAvailableCopies(),
                book.getLostCopies(), book.getDamagedCopies(), book.getImageUrl()
        );
    }
}
