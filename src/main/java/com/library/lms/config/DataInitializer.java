package com.library.lms.config;

import com.library.lms.entity.Permission;
import com.library.lms.entity.Role;
import com.library.lms.entity.User;
import com.library.lms.repository.PermissionRepository;
import com.library.lms.repository.RoleRepository;
import com.library.lms.repository.UserRepository;
import com.library.lms.security.Permissions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Runs once at application startup. Seeds:
 *  1. Every Permission in Permissions.ALL_PERMISSIONS
 *  2. Three default roles: ROLE_ADMIN (all permissions), ROLE_LIBRARIAN, ROLE_MEMBER
 *  3. A single bootstrap admin user (only if no admin exists yet) with full permissions,
 *     so the very first login to the system is always possible without a chicken-and-egg
 *     "who creates the first user" problem. All subsequent users must be created by an
 *     authenticated admin via POST /api/admin/users.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.default-username}")
    private String defaultAdminUsername;

    @Value("${app.admin.default-email}")
    private String defaultAdminEmail;

    @Value("${app.admin.default-password}")
    private String defaultAdminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        Set<Permission> allPermissions = seedPermissions();
        Role adminRole = seedRole(Permissions.ROLE_ADMIN, "Full system access", allPermissions);
        seedRole(Permissions.ROLE_LIBRARIAN, "Manages books and borrow records",
                filterPermissions(allPermissions, Permissions.LIBRARIAN_PERMISSIONS));
        seedRole(Permissions.ROLE_MEMBER, "Regular library member",
                filterPermissions(allPermissions, Permissions.MEMBER_PERMISSIONS));

        seedDefaultAdminUser(adminRole);
    }

    private Set<Permission> seedPermissions() {
        Set<Permission> result = new HashSet<>();
        for (String name : Permissions.ALL_PERMISSIONS) {
            Permission permission = permissionRepository.findByName(name)
                    .orElseGet(() -> permissionRepository.save(
                            Permission.builder().name(name).description(name.replace('_', ' ')).build()));
            result.add(permission);
        }
        return result;
    }

    private Set<Permission> filterPermissions(Set<Permission> all, Set<String> names) {
        return all.stream().filter(p -> names.contains(p.getName())).collect(Collectors.toSet());
    }

    private Role seedRole(String name, String description, Set<Permission> permissions) {
        return roleRepository.findByName(name)
                .map(existing -> {
                    // Keep ROLE_ADMIN's permission set in sync with the full catalogue
                    // on every startup, so newly added permissions are auto-granted to admins.
                    if (name.equals(Permissions.ROLE_ADMIN)) {
                        existing.setPermissions(permissions);
                        return roleRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    Role role = Role.builder().name(name).description(description).permissions(permissions).build();
                    Role saved = roleRepository.save(role);
                    log.info("Seeded role: {}", name);
                    return saved;
                });
    }

    private void seedDefaultAdminUser(Role adminRole) {
        boolean adminExists = userRepository.findAll().stream()
                .anyMatch(u -> u.getRoles().stream().anyMatch(r -> r.getName().equals(Permissions.ROLE_ADMIN)));

        if (adminExists) {
            return;
        }

        User admin = User.builder()
                .username(defaultAdminUsername)
                .email(defaultAdminEmail)
                .password(passwordEncoder.encode(defaultAdminPassword))
                .fullName("System Administrator")
                .enabled(true)
                .roles(Set.of(adminRole))
                .build();

        userRepository.save(admin);

        log.warn("=================================================================");
        log.warn(" Default admin user created:");
        log.warn("   username: {}", defaultAdminUsername);
        log.warn("   password: {}", defaultAdminPassword);
        log.warn(" CHANGE THIS PASSWORD IMMEDIATELY (or set ADMIN_PASSWORD env var).");
        log.warn("=================================================================");
    }
}
