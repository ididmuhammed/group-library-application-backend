package com.library.lms.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record BookRequest(
        @NotBlank(message = "Title is required") String title,
        @NotBlank(message = "Author is required") String author,
        String isbn,
        String category,
        @Min(value = 1, message = "Total copies must be at least 1") Integer totalCopies
) {}
