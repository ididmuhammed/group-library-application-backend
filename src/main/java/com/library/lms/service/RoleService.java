package com.library.lms.service;

import com.library.lms.dto.request.CreateRoleRequest;
import com.library.lms.dto.response.RoleResponse;
import com.library.lms.entity.Permission;
import com.library.lms.entity.Role;
import com.library.lms.exception.BadRequestException;
import com.library.lms.exception.ConflictException;
import com.library.lms.exception.ResourceNotFoundException;
import com.library.lms.repository.PermissionRepository;
import com.library.lms.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        if (roleRepository.existsByName(request.name())) {
            throw new ConflictException("Role already exists: " + request.name());
        }

        Set<Permission> permissions = resolvePermissions(request.permissionNames());

        Role role = Role.builder()
                .name(request.name())
                .description(request.description())
                .permissions(permissions)
                .build();

        return RoleResponse.from(roleRepository.save(role));
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream().map(RoleResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getAllPermissionNames() {
        return permissionRepository.findAll().stream().map(Permission::getName).sorted().collect(Collectors.toList());
    }

    @Transactional
    public RoleResponse updateRolePermissions(Long roleId, Set<String> permissionNames) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        if (role.getName().equals("ROLE_ADMIN")) {
            throw new BadRequestException("The built-in ROLE_ADMIN permission set cannot be modified");
        }

        role.setPermissions(resolvePermissions(permissionNames));
        return RoleResponse.from(roleRepository.save(role));
    }

    private Set<Permission> resolvePermissions(Set<String> names) {
        Set<Permission> permissions = new HashSet<>();
        for (String name : names) {
            Permission permission = permissionRepository.findByName(name)
                    .orElseThrow(() -> new BadRequestException("Unknown permission: " + name));
            permissions.add(permission);
        }
        return permissions;
    }
}
