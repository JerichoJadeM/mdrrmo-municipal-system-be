package com.isufst.mdrrmosystem.entity;

import jakarta.persistence.Embeddable;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;

@Embeddable
public class Authority implements GrantedAuthority {

    private String authority;

    public Authority () {}

    public Authority(String authority) {
        this.authority = authority;
    }

    @Override
    public @Nullable String getAuthority() {
        return authority;
    }
}
