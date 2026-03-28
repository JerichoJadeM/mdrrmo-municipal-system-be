package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.request.AdminUserUpdateRequest;
import com.isufst.mdrrmosystem.request.CreateUserRequest;
import com.isufst.mdrrmosystem.request.UpdateUserRoleRequest;
import com.isufst.mdrrmosystem.response.AdminUserResponse;

import java.util.List;

public interface AdminUserService {
    List<AdminUserResponse> getAllUsers();
    AdminUserResponse getUser(Long id);
    AdminUserResponse createUser(CreateUserRequest request);
    AdminUserResponse updateUser(Long id, AdminUserUpdateRequest request);
    AdminUserResponse updateRoles(Long id, UpdateUserRoleRequest request);
    void deleteUser(Long id);
}
