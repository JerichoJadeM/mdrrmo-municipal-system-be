package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.request.PasswordUpdateRequest;
import com.isufst.mdrrmosystem.response.UserResponse;

public interface UserService {

    UserResponse getUserInfo();
    void deleteUser();

    void updatePassword(PasswordUpdateRequest passwordUpdateRequest);
}
