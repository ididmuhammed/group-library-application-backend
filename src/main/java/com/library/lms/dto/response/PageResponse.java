package com.library.lms.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Thin, stable wrapper around Spring's {@link Page} so the API doesn't leak
 * Spring Data internals (pageable, sort objects, etc.) to the frontend.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
