package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.request.AdminUserUpdateRequest;
import com.isufst.mdrrmosystem.request.CreateUserRequest;
import com.isufst.mdrrmosystem.request.UpdateUserRoleRequest;
import com.isufst.mdrrmosystem.response.AdminUserResponse;
import com.isufst.mdrrmosystem.service.AdminUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public List<AdminUserResponse> getAllUsers() {
        return adminUserService.getAllUsers();
    }

    @GetMapping("/{id}")
    public AdminUserResponse getUser(@PathVariable Long id) {
        return adminUserService.getUser(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public AdminUserResponse createUser(@RequestBody CreateUserRequest request) {
        return adminUserService.createUser(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public AdminUserResponse updateUser(@PathVariable Long id,
                                        @RequestBody AdminUserUpdateRequest request) {
        return adminUserService.updateUser(id, request);
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public AdminUserResponse updateRoles(@PathVariable Long id,
                                         @RequestBody UpdateUserRoleRequest request) {
        return adminUserService.updateRoles(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(@PathVariable Long id) {
        adminUserService.deleteUser(id);
    }

}