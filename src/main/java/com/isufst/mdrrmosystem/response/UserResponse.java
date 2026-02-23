package com.isufst.mdrrmosystem.response;

import com.isufst.mdrrmosystem.entity.Authority;

import java.util.List;

public class UserResponse {

    private long id;
    private String fullName;
    private String number;
    private String email;
    private List<Authority> authorities;

    public UserResponse(long id, String fullName, String number, String email, List<Authority> authorities) {
        this.id = id;
        this.fullName = fullName;
        this.number = number;
        this.email = email;
        this.authorities = authorities;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public List<Authority> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(List<Authority> authorities) {
        this.authorities = authorities;
    }
}
