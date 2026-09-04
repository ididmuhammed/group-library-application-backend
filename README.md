# Library Management System — Backend

Spring Boot 3 + Spring Security 6 + JWT backend for a library management system,
with role/permission-based authorization and an auto-seeded default admin.

## How the permission model works

- **Permission** — an atomic capability, e.g. `BOOK_CREATE`, `USER_MANAGE_ROLES`.
- **Role** — a named bundle of permissions, e.g. `ROLE_ADMIN`, `ROLE_LIBRARIAN`, `ROLE_MEMBER`.
- **User** — has one or more roles. Their effective authorities = union of all
  permissions from all their roles (plus the role names themselves, so both
  `hasRole('ADMIN')` and `hasAuthority('BOOK_CREATE')` style checks work).
- On login, the JWT **access token** embeds the resolved authority list, so
  every request is authorized from the token alone — no DB hit needed per request.

### Bootstrap flow (the "chicken and egg" problem)

1. On first startup, `DataInitializer` seeds every `Permission`, three default
   `Role`s (`ROLE_ADMIN` with **all** permissions, `ROLE_LIBRARIAN`, `ROLE_MEMBER`),
   and — **only if no admin user exists yet** — a default admin account:
   - username: `admin` (override with `ADMIN_USERNAME`)
   - password: `Admin@123` (override with `ADMIN_PASSWORD`) — **change this immediately**
2. Log in as that admin via `POST /api/auth/login` to get a JWT.
3. Use that JWT to create further users via `POST /api/admin/users`, assigning
   them whichever roles (and therefore permissions) you like.
4. Optionally define new roles / tweak permission sets via `/api/admin/roles`.

The default admin is only ever (re)created when the system has zero admin users —
so it's safe to restart the app without duplicating or resetting it.

## Running it

Zero setup, in-memory H2:

```bash
mvn spring-boot:run
```

The app starts on `http://localhost:8080`. Swagger UI: `http://localhost:8080/swagger-ui.html`.
H2 console (dev only): `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:librarydb`, user `sa`, no password).

To use MySQL instead:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql \
  -DDB_USERNAME=root -DDB_PASSWORD=yourpassword
```

(or set `DB_USERNAME` / `DB_PASSWORD` / `JWT_SECRET` / `ADMIN_PASSWORD` as environment variables —
**always override `JWT_SECRET` and `ADMIN_PASSWORD` in production**.)

## API overview

### Auth (public)
| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/login` | `{ "username", "password" }` → access + refresh tokens |
| POST | `/api/auth/refresh` | `{ "refreshToken" }` → new token pair |
| GET | `/api/auth/me` | current authenticated user (requires `Authorization: Bearer <token>`) |

### Admin — user management (requires permissions)
| Method | Path | Permission required |
|---|---|---|
| POST | `/api/admin/users` | `USER_CREATE` |
| GET | `/api/admin/users` | `USER_READ` |
| GET | `/api/admin/users/{id}` | `USER_READ` |
| PUT | `/api/admin/users/{id}/roles` | `USER_MANAGE_ROLES` |
| PATCH | `/api/admin/users/{id}/enabled?enabled=true|false` | `USER_UPDATE` |
| DELETE | `/api/admin/users/{id}` | `USER_DELETE` |

### Admin — role/permission management
| Method | Path | Permission required |
|---|---|---|
| POST | `/api/admin/roles` | `ROLE_MANAGE` |
| GET | `/api/admin/roles` | `ROLE_MANAGE` |
| GET | `/api/admin/roles/permissions` | `ROLE_MANAGE` |
| PUT | `/api/admin/roles/{id}/permissions` | `ROLE_MANAGE` |

### Books
| Method | Path | Permission required |
|---|---|---|
| POST | `/api/books` | `BOOK_CREATE` |
| GET | `/api/books` | `BOOK_READ` |
| GET | `/api/books/{id}` | `BOOK_READ` |
| PUT | `/api/books/{id}` | `BOOK_UPDATE` |
| DELETE | `/api/books/{id}` | `BOOK_DELETE` |
| POST | `/api/books/{id}/borrow` | `BOOK_BORROW` |
| POST | `/api/books/borrow-records/{recordId}/return` | `BOOK_RETURN` |

## Example: bootstrap and create a librarian

```bash
# 1. Log in as the seeded admin
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123"}'
# => { "accessToken": "...", "refreshToken": "...", ... }

TOKEN="<paste accessToken here>"

# 2. Create a librarian user
curl -s -X POST http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
        "username": "jane.lib",
        "email": "jane@library.local",
        "password": "Librarian@123",
        "fullName": "Jane Doe",
        "roleNames": ["ROLE_LIBRARIAN"]
      }'

# 3. Create a plain member
curl -s -X POST http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
        "username": "john.member",
        "email": "john@library.local",
        "password": "Member@123",
        "fullName": "John Smith",
        "roleNames": ["ROLE_MEMBER"]
      }'
```

