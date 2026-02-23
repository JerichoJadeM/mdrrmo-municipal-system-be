package com.isufst.mdrrmosystem.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotEmpty(message = "First name is mandatory")
    @Size(min = 1, max = 50, message = "First name must be at least 1 characters long")
    private String firstName;

    @NotEmpty(message = "Middle name is mandatory")
    @Size(min = 1, max = 50, message = "Midlle name must be at least 1 characters long")
    private String middleName;

    @NotEmpty(message = "Last name is mandatory")
    @Size(min = 1, max = 50, message = "Last name must be at least 1 characters long")
    private String lastName;

    @NotEmpty(message = "Number is mandatory")
    @Pattern(regexp = "^09\\d{9}$", message = "Must be a valid 11-digit PH mobile number")
    private String number;

    @NotEmpty(message = "Email is mandatory")
    @Email(message = "Invalid email format")
    private String email;

    @NotEmpty(message = "Password is mandatory")
    @Size(min=8, max=30, message = "Password must be at least 8 characters long")
    private String password;

    public RegisterRequest(String firstName, String middleName, String lastName, String number, String email, String password) {
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.number = number;
        this.email = email;
        this.password = password;

    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
