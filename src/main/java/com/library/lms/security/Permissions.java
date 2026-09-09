package com.library.lms.security;

import java.util.Set;

/**
 * Central catalogue of permission and role names.
 * The DataInitializer seeds the Permission table from PERMISSION_DESCRIPTIONS
 * and wires ROLE_ADMIN to all of them.
 */
public final class Permissions {

    private Permissions() {}

    // ---- Roles ----
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_LIBRARIAN = "ROLE_LIBRARIAN";
    public static final String ROLE_MEMBER = "ROLE_MEMBER";

    // ---- User management ----
    public static final String USER_CREATE = "USER_CREATE";
    public static final String USER_READ = "USER_READ";
    public static final String USER_UPDATE = "USER_UPDATE";
    public static final String USER_DELETE = "USER_DELETE";
    public static final String USER_MANAGE_ROLES = "USER_MANAGE_ROLES";

    // ---- Role/Permission management ----
    public static final String ROLE_MANAGE = "ROLE_MANAGE";

    // ---- Book management ----
    public static final String BOOK_CREATE = "BOOK_CREATE";
    public static final String BOOK_READ = "BOOK_READ";
    public static final String BOOK_UPDATE = "BOOK_UPDATE";
    public static final String BOOK_DELETE = "BOOK_DELETE";


    // ---- Borrowing ----
    public static final String BOOK_BORROW = "BOOK_BORROW";
    public static final String BOOK_RETURN = "BOOK_RETURN";
    public static final String BORROW_RECORD_READ_ALL = "BORROW_RECORD_READ_ALL";

    // ---- Reservations ----
    public static final String BOOK_RESERVE = "BOOK_RESERVE";
    public static final String RESERVATION_READ_ALL = "RESERVATION_READ_ALL";

    // ---- Fines ----
    public static final String FINE_READ = "FINE_READ";
    public static final String FINE_MANAGE = "FINE_MANAGE";

    public static final Set<String> ALL_PERMISSIONS = Set.of(
            USER_CREATE, USER_READ, USER_UPDATE, USER_DELETE, USER_MANAGE_ROLES,
            ROLE_MANAGE,
            BOOK_CREATE, BOOK_READ, BOOK_UPDATE, BOOK_DELETE,
            BOOK_BORROW, BOOK_RETURN, BORROW_RECORD_READ_ALL,
            BOOK_RESERVE, RESERVATION_READ_ALL,
            FINE_READ, FINE_MANAGE
    );

    public static final Set<String> LIBRARIAN_PERMISSIONS = Set.of(
            BOOK_CREATE, BOOK_READ, BOOK_UPDATE, BOOK_DELETE,
            BOOK_RETURN, BORROW_RECORD_READ_ALL, USER_READ,
            RESERVATION_READ_ALL, FINE_READ, FINE_MANAGE
    );

    public static final Set<String> MEMBER_PERMISSIONS = Set.of(
            BOOK_READ, BOOK_BORROW, BOOK_RETURN, BOOK_RESERVE
    );
}
