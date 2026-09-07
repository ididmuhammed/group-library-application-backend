package com.library.lms.service;

import com.library.lms.dto.request.CreateUserRequest;
import com.library.lms.dto.request.UpdateUserRolesRequest;
import com.library.lms.dto.response.UserResponse;
import com.library.lms.entity.Role;
import com.library.lms.entity.User;
import com.library.lms.exception.BadRequestException;
import com.library.lms.exception.ConflictException;
import com.library.lms.exception.ResourceNotFoundException;
import com.library.lms.repository.RoleRepository;
import com.library.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Used by an authenticated admin (USER_CREATE permission) to create
     * a new user and assign roles. This is the only way non-admin users
     * get created; there is no open self-registration endpoint.
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {

        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException(
                    "Username already taken: " + request.username()
            );
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException(
                    "Email already registered: " + request.email()
            );
        }

        Set<Role> roles = resolveRoles(request.roleNames());

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .enabled(true)
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);

        // Send account creation email
        emailService.sendUserCreatedEmail(
                savedUser.getEmail(),
                savedUser.getFullName(),
                savedUser.getUsername(),
                request.password(),
                savedUser.getRoles()
                        .stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet())
        );

        return UserResponse.from(savedUser);
    }

    @Transactional
    public UserResponse updateUserRoles(Long userId, UpdateUserRolesRequest request) {
        User user = getUserEntity(userId);
        user.setRoles(resolveRoles(request.roleNames()));
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse setEnabled(Long userId, boolean enabled) {
        User user = getUserEntity(userId);
        user.setEnabled(enabled);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(UserResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        return UserResponse.from(getUserEntity(userId));
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = getUserEntity(userId);
        boolean isLastAdmin = user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"))
                && userRepository.findAll().stream()
                    .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN")))
                    .count() <= 1;
        if (isLastAdmin) {
            throw new BadRequestException("Cannot delete the last remaining admin user");
        }
        userRepository.delete(user);
    }

    private User getUserEntity(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private Set<Role> resolveRoles(Set<String> roleNames) {
        Set<Role> roles = new HashSet<>();
        for (String name : roleNames) {
            Role role = roleRepository.findByName(name)
                    .orElseThrow(() -> new BadRequestException("Unknown role: " + name));
            roles.add(role);
        }
        return roles;
    }
}
