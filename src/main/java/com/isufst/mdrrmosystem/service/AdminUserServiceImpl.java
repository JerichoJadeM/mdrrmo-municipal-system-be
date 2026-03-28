package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Authority;
import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.repository.UserRepository;
import com.isufst.mdrrmosystem.request.AdminUserUpdateRequest;
import com.isufst.mdrrmosystem.request.CreateUserRequest;
import com.isufst.mdrrmosystem.request.UpdateUserRoleRequest;
import com.isufst.mdrrmosystem.response.AdminUserResponse;
import com.isufst.mdrrmosystem.util.FindAuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminUserServiceImpl implements AdminUserService {
    private final UserRepository userRepository;
    private final FindAuthenticatedUser findAuthenticatedUser;
    private final AdminAuditService adminAuditService;
    private final PasswordEncoder passwordEncoder;

    public AdminUserServiceImpl(UserRepository userRepository, FindAuthenticatedUser findAuthenticatedUser, AdminAuditService adminAuditService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.findAuthenticatedUser = findAuthenticatedUser;
        this.adminAuditService = adminAuditService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override public List<AdminUserResponse> getAllUsers() { return userRepository.findAll().stream().map(this::map).toList(); }
    @Override public AdminUserResponse getUser(Long id) { return map(findUser(id)); }

    @Override
    @Transactional
    public AdminUserResponse createUser(CreateUserRequest request) {
        User actor = findAuthenticatedUser.getAuthenticatedUser();

        if (request.email() == null || request.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }

        if (request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }

        userRepository.findByEmail(request.email().trim())
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
                });

        User user = new User();
        user.setFirstName(request.firstName() != null ? request.firstName().trim() : "");
        user.setMiddleName(request.middleName() != null ? request.middleName().trim() : "");
        user.setLastName(request.lastName() != null ? request.lastName().trim() : "");
        user.setEmail(request.email().trim());
        user.setNumber(request.number() != null ? request.number().trim() : "");
        user.setPassword(passwordEncoder.encode(request.password().trim()));

        user.setPosition(request.position() != null ? request.position().trim() : "");
        user.setOffice(request.office() != null ? request.office().trim() : "MDRRMO");
        user.setAccountStatus(request.accountStatus() != null ? request.accountStatus().trim().toUpperCase() : "ACTIVE");
        user.setResponderEligible(Boolean.TRUE.equals(request.responderEligible()));
        user.setCoordinatorEligible(Boolean.TRUE.equals(request.coordinatorEligible()));

        if (request.authorities() == null || request.authorities().isEmpty()) {
            user.setAuthorities(List.of(new Authority("ROLE_USER")));
        } else {
            List<Authority> authorities;
            if (request.authorities() == null || request.authorities().isEmpty()) {
                authorities = new ArrayList<>();
                authorities.add(new Authority("ROLE_USER"));
            } else {
                authorities = request.authorities().stream()
                        .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                        .map(String::toUpperCase)
                        .distinct()
                        .map(Authority::new)
                        .collect(Collectors.toCollection(ArrayList::new));
            }

            user.setAuthorities(authorities);
        }

        if (user.getAssignmentStatus() == null || user.getAssignmentStatus().isBlank()) {
            user.setAssignmentStatus("AVAILABLE");
        }

        User saved = userRepository.save(user);

        adminAuditService.log(
                actor,
                saved,
                "USER_CREATE",
                actor.getFullName() + " created user " + saved.getFullName()
        );

        return map(saved);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User actor = findAuthenticatedUser.getAuthenticatedUser();
        User user = findUser(id);

        if (actor.getId() == user.getId()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete your own account");
        }

        userRepository.delete(user);

        adminAuditService.log(
                actor,
                user,
                "USER_DELETE",
                actor.getFullName() + " deleted user " + user.getFullName()
        );
    }

    @Override
    @Transactional
    public AdminUserResponse updateUser(Long id, AdminUserUpdateRequest request) {
        User actor = findAuthenticatedUser.getAuthenticatedUser();
        User user = findUser(id);
        if (request.position() != null) user.setPosition(request.position());
        if (request.office() != null) user.setOffice(request.office());
        if (request.accountStatus() != null) user.setAccountStatus(request.accountStatus().trim().toUpperCase());
        if (request.responderEligible() != null) user.setResponderEligible(request.responderEligible());
        if (request.coordinatorEligible() != null) user.setCoordinatorEligible(request.coordinatorEligible());
        if (request.profileImageUrl() != null) user.setProfileImageUrl(request.profileImageUrl());
        User saved = userRepository.save(user);
        adminAuditService.log(actor, saved, "USER_UPDATE", actor.getFullName() + " updated user profile settings for " + saved.getFullName());
        return map(saved);
    }

    @Override
    @Transactional
    public AdminUserResponse updateRoles(Long id, UpdateUserRoleRequest request) {
        User actor = findAuthenticatedUser.getAuthenticatedUser();
        User user = findUser(id);

        if (request.authorities() == null || request.authorities().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one authority is required");
        }

        List<Authority> newAuthorities = request.authorities().stream()
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(String::toUpperCase)
                .distinct()
                .map(Authority::new)
                .collect(Collectors.toCollection(ArrayList::new));

        boolean removingAdminRole =
                user.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))
                        && newAuthorities.stream().noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (removingAdminRole && userRepository.countUsersByAuthority("ROLE_ADMIN") <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot demote the last remaining admin.");
        }

        user.getAuthorities().clear();
        user.updateAuthorities(newAuthorities);

        User saved = userRepository.save(user);

        adminAuditService.log(
                actor,
                saved,
                "ROLE_UPDATE",
                actor.getFullName() + " updated authorities for " + saved.getFullName()
        );

        return map(saved);
    }

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private AdminUserResponse map(User user) {
        List<String> authorities = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        return new AdminUserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getNumber(),
                user.getProfileImageUrl(), user.getPosition(), user.getOffice(), user.getAccountStatus(),
                user.getAssignmentStatus(), user.getResponderEligible(), user.getCoordinatorEligible(), authorities);
    }
}
