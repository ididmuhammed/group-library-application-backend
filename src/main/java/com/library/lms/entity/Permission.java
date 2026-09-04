package com.library.lms.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * A fine-grained permission, e.g. "BOOK_CREATE", "USER_MANAGE".
 * Roles are collections of permissions; the JWT ultimately carries
 * the resolved set of permission names as authorities.
 */
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permission that)) return false;
        return name != null && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
