package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.request.AuthenticationRequest;
import com.isufst.mdrrmosystem.request.RegisterRequest;
import com.isufst.mdrrmosystem.response.AuthenticationResponse;
import com.isufst.mdrrmosystem.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication REST API Endpoints", description = "Operations related to register and login")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Operation(summary = "Register a new user", description = "Create new user and save in the database")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register")
    public void register(@Valid @RequestBody RegisterRequest registerRequest) throws Exception{
        authenticationService.register(registerRequest);
    }

    @Operation(summary = "Login a user", description = "Submit email and password to authenticate a user")
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/login")
    public AuthenticationResponse login(@Valid @RequestBody AuthenticationRequest authRequest) {
        return authenticationService.login(authRequest);
    }
}
