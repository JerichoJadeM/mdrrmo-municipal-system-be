package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.response.ResponderResponse;
import com.isufst.mdrrmosystem.response.UserResponse;
import com.isufst.mdrrmosystem.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/responders")
public class ResponderController {

    private final UserService userService;

    public ResponderController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/available")
    public List<ResponderResponse> searchAvailableResponders(
            @RequestParam(required = false, defaultValue = "") String search) {
        return userService.searchAvailableResponders(search);
    }
}
