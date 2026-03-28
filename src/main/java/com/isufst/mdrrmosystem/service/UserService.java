package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.request.PasswordUpdateRequest;
import com.isufst.mdrrmosystem.request.UpdateMyProfileRequest;
import com.isufst.mdrrmosystem.response.AssignableUserResponse;
import com.isufst.mdrrmosystem.response.ResponderResponse;
import com.isufst.mdrrmosystem.request.UpdateProfilePhotoRequest;
import com.isufst.mdrrmosystem.response.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse getUserInfo();
    UserResponse updateProfilePhoto(UpdateProfilePhotoRequest request);
    void deleteUser();

    void updatePassword(PasswordUpdateRequest passwordUpdateRequest);

    List<ResponderResponse> searchAvailableResponders(String search);

    List<AssignableUserResponse> getAssignableResponders();

    List<AssignableUserResponse> getAssignableCoordinators();

    UserResponse updateMyProfile(UpdateMyProfileRequest request);
}
