package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.request.PasswordUpdateRequest;
import com.isufst.mdrrmosystem.response.AssignableUserResponse;
import com.isufst.mdrrmosystem.response.UserResponse;
import com.isufst.mdrrmosystem.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User REST API Endpoints", description = "Operations related to info about current user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/info")
    public UserResponse getInfo() {
        return userService.getUserInfo();
    }

    @DeleteMapping
    public void deleteUser() {
        userService.deleteUser();
    }

    @PutMapping("/password")
    public void updatePassword(@Valid @RequestBody PasswordUpdateRequest passwordUpdateRequest) {
        userService.updatePassword(passwordUpdateRequest);
    }

    @GetMapping("/responders")
    public List<AssignableUserResponse> getResponders() {
        return userService.getAssignableResponders();
    }

    @GetMapping("/coordinators")
    public List<AssignableUserResponse> getCoordinators() {
        return userService.getAssignableCoordinators();
    }
}