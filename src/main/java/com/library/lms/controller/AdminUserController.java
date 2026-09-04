package com.library.lms.controller;

import com.library.lms.dto.request.CreateUserRequest;
import com.library.lms.dto.request.UpdateUserRolesRequest;
import com.library.lms.dto.response.UserResponse;
import com.library.lms.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Only reachable by users holding the relevant permission (normally just ROLE_ADMIN,
 * since USER_CREATE / USER_MANAGE_ROLES / USER_DELETE are admin-only permissions
 * by default — but any role granted those permissions can use these endpoints too).
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('USER_MANAGE_ROLES')")
    public ResponseEntity<UserResponse> updateRoles(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateUserRolesRequest request) {
        return ResponseEntity.ok(userService.updateUserRoles(id, request));
    }

    @PatchMapping("/{id}/enabled")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<UserResponse> setEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        return ResponseEntity.ok(userService.setEnabled(id, enabled));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
