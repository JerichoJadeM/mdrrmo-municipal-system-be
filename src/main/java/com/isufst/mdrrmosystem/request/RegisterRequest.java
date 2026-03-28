package com.isufst.mdrrmosystem.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RegisterRequest(

    @NotEmpty(message = "First name is mandatory")
    @Size(min = 1, max = 50, message = "First name must be at least 1 characters long")
    String firstName,

    @NotEmpty(message = "Middle name is mandatory")
    @Size(min = 1, max = 50, message = "Midlle name must be at least 1 characters long")
    String middleName,

    @NotEmpty(message = "Last name is mandatory")
    @Size(min = 1, max = 50, message = "Last name must be at least 1 characters long")
    String lastName,

    @NotEmpty(message = "Number is mandatory")
    @Pattern(regexp = "^09\\d{9}$", message = "Must be a valid 11-digit PH mobile number")
    String number,

    @NotEmpty(message = "Email is mandatory")
    @Email(message = "Invalid email format")
    String email,

    @NotEmpty(message = "Password is mandatory")
    @Size(min=8, max=30, message = "Password must be at least 8 characters long")
    String password,

    String position,
    String office,
    String accountStatus,
    Boolean responderEligible,
    Boolean coordinatorEligible,
    List<String> authorities
)

{}
