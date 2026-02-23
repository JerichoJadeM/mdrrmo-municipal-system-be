package com.isufst.mdrrmosystem.util;

import com.isufst.mdrrmosystem.entity.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class FindAuthenticatedUserImpl implements FindAuthenticatedUser {

    @Override
    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication == null || !authentication.isAuthenticated() ||
                Objects.equals(authentication.getPrincipal(), "anonymousUser")) {
            throw new AccessDeniedException("Authentication required");
        }

        return (User) authentication.getPrincipal();
    }
}
