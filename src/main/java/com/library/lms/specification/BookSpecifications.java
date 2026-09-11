package com.library.lms.specification;

import com.library.lms.entity.Book;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class BookSpecifications {

    private BookSpecifications() {
    }

    /**
     * Builds the combined filter for GET /api/books.
     *
     * @param search       free-text match against title, author, ISBN and category
     * @param category     exact category filter (case-insensitive), applied in addition to search
     * @param availableOnly when true, only returns books with availableCopies > 0
     */
    public static Specification<Book> withFilters(String search, String category, Boolean availableOnly) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("author")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("isbn"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("category"), "")), like)
                ));
            }

            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase()));
            }

            if (Boolean.TRUE.equals(availableOnly)) {
                predicates.add(cb.greaterThan(root.get("availableCopies"), 0));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
