package com.library.lms.controller;

import com.library.lms.dto.request.BookRequest;
import com.library.lms.dto.response.BookResponse;
import com.library.lms.dto.response.BorrowRecordResponse;
import com.library.lms.dto.response.PageResponse;
import com.library.lms.dto.response.ReservationResponse;
import com.library.lms.repository.BorrowRecordRepository;
import com.library.lms.security.UserPrincipal;
import com.library.lms.service.BookService;
import com.library.lms.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class  BookController {

    private final BookService bookService;
    private final BorrowRecordRepository borrowRecordRepository;
    private final ReservationService reservationService;

    /**
     * Creates a book. Send multipart/form-data with a "book" part (JSON body
     * matching BookRequest) and, if you want a cover image, an "image" file part.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('BOOK_CREATE')")
    public ResponseEntity<BookResponse> createBook(
            @Valid @RequestPart("book") BookRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookService.createBook(request, image));
    }

    /**
     * Paged/sorted/filtered catalog listing.
     * Query params: page, size, sort (e.g. sort=title,asc&sort=author,desc - repeatable),
     * search (matches title/author/isbn/category), category (exact), availableOnly (boolean).
     * Sortable fields: id, title, author, isbn, category, totalCopies, availableCopies, lostCopies, damagedCopies.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('BOOK_READ')")
    public ResponseEntity<PageResponse<BookResponse>> getAllBooks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean availableOnly,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(bookService.getAllBooks(search, category, availableOnly, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BOOK_READ')")
    public ResponseEntity<BookResponse> getBook(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBook(id));
    }

    /**
     * Updates a book. The "image" part is optional; when present it replaces
     * the existing cover image (and deletes the old one from Cloudinary).
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('BOOK_UPDATE')")
    public ResponseEntity<BookResponse> updateBook(
            @PathVariable Long id,
            @Valid @RequestPart("book") BookRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.ok(bookService.updateBook(id, request, image));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('BOOK_DELETE')")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/borrow")
    @PreAuthorize("hasAuthority('BOOK_BORROW')")
    public ResponseEntity<Void> borrowBook(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        bookService.borrowBook(id, principal.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/borrow-records/{recordId}/return")
    @PreAuthorize("hasAuthority('BOOK_RETURN')")
    public ResponseEntity<Void> returnBook(@PathVariable Long recordId, @AuthenticationPrincipal UserPrincipal principal) {
        bookService.returnBook(recordId, principal.getUsername());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my-loans")
    @PreAuthorize("hasAuthority('BOOK_BORROW')")
    public ResponseEntity<List<BorrowRecordResponse>> getMyLoans(@AuthenticationPrincipal UserPrincipal principal) {
        List<BorrowRecordResponse> records = bookService.getMyBorrowedBooks(principal.getUsername())
                .stream().map(BorrowRecordResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(records);
    }

    @GetMapping("/borrow-records")
    @PreAuthorize("hasAuthority('BORROW_RECORD_READ_ALL')")
    public ResponseEntity<List<?>> getAllLoans(@AuthenticationPrincipal UserPrincipal principal) {
        List<?> records = borrowRecordRepository.findAll();
        return ResponseEntity.ok(records);
    }

    @PostMapping("/{id}/reserve")
    @PreAuthorize("hasAuthority('BOOK_RESERVE')")
    public ResponseEntity<ReservationResponse> reserveBook(@PathVariable Long id,
                                                           @AuthenticationPrincipal UserPrincipal principal) {
        ReservationResponse response = ReservationResponse.from(
                reservationService.reserveBook(id, principal.getUsername()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
