package com.library.lms.controller;

import com.library.lms.dto.request.CreateRoleRequest;
import com.library.lms.dto.response.RoleResponse;
import com.library.lms.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_MANAGE')")
public class AdminRoleController {

    private final RoleService roleService;

    @PostMapping
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.createRole(request));
    }

    @GetMapping
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/permissions")
    public ResponseEntity<List<String>> getAllPermissions() {
        return ResponseEntity.ok(roleService.getAllPermissionNames());
    }

    @PutMapping("/{id}/permissions")
    public ResponseEntity<RoleResponse> updatePermissions(@PathVariable Long id, @RequestBody Set<String> permissionNames) {
        return ResponseEntity.ok(roleService.updateRolePermissions(id, permissionNames));
    }
}
