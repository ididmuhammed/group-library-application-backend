package com.library.lms.specification;

import com.library.lms.entity.Role;
import com.library.lms.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    /**
     * Builds the combined filter for GET /api/admin/users.
     *
     * @param search  free-text match against username, email and full name
     * @param role    exact role name filter, e.g. "ROLE_ADMIN"
     * @param enabled account-enabled filter
     */
    public static Specification<User> withFilters(String search, String role, Boolean enabled) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("fullName"), "")), like)
                ));
            }

            if (role != null && !role.isBlank()) {
                Join<User, Role> roleJoin = root.join("roles");
                predicates.add(cb.equal(cb.upper(roleJoin.get("name")), role.trim().toUpperCase()));
                query.distinct(true); // avoid duplicate rows from the roles join
            }

            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
