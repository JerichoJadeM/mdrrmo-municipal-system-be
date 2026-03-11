package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Authority;
import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.repository.UserRepository;
import com.isufst.mdrrmosystem.request.PasswordUpdateRequest;
import com.isufst.mdrrmosystem.response.AssignableUserResponse;
import com.isufst.mdrrmosystem.response.ResponderResponse;
import com.isufst.mdrrmosystem.response.UserResponse;
import com.isufst.mdrrmosystem.util.FindAuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final FindAuthenticatedUser findAuthenticatedUser;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,  FindAuthenticatedUser findAuthenticatedUser,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.findAuthenticatedUser = findAuthenticatedUser;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserInfo() {
        User user = findAuthenticatedUser.getAuthenticatedUser();

        List<String> authorities = user.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getNumber(),
                user.getEmail(),
                authorities
        );
    }

    @Override
    @Transactional
    public void deleteUser() {
        User user = findAuthenticatedUser.getAuthenticatedUser();

        if(isLastAdmin(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin cannot delete itself");
        }

        userRepository.delete(user);
    }

    @Override
    @Transactional
    public void updatePassword(PasswordUpdateRequest passwordUpdateRequest) {
        User user = findAuthenticatedUser.getAuthenticatedUser();

        if(!isOldPasswordCorrect(user.getPassword(), passwordUpdateRequest.getOldPassword())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current Password is incorrect");
        }

        if(!isNewPasswordConfirmed(passwordUpdateRequest.getNewPassword(), passwordUpdateRequest.getNewPassword2())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New Password do not match");
        }

        if(!isNewPasswordDifferent(passwordUpdateRequest.getOldPassword(), passwordUpdateRequest.getNewPassword())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Old and new password must be different");
        }

        user.setPassword(passwordEncoder.encode(passwordUpdateRequest.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponderResponse> searchAvailableResponders(String search) {
        String keyword = search == null ? "" : search.trim();

        return userRepository
                .findAvailableResponders(keyword)
                .stream()
                .map(this::mapToResponderResponse)
                .toList();
    }

    @Override
    public List<AssignableUserResponse> getAssignableCoordinators() {
        return userRepository.findAssignableCoordinators()
                .stream()
                .map(this::mapAssignableUser)
                .toList();
    }

    @Override
    public List<AssignableUserResponse> getAssignableResponders() {
        return userRepository.findAvailableResponders()
                .stream()
                .map(this::mapAssignableUser)
                .toList();
    }

    private AssignableUserResponse mapAssignableUser(User user) {
        return new AssignableUserResponse(
                user.getId(),
                user.getFullName(),
                user.getAssignmentStatus()
        );
    }

    private ResponderResponse mapToResponderResponse(User user) {
        return new ResponderResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getFirstName() + " " + user.getLastName()
        );
    }

    private boolean isOldPasswordCorrect(String currentPassword, String oldPassword){
        return passwordEncoder.matches(oldPassword, currentPassword);
    }

    private boolean isNewPasswordConfirmed(String newPassword, String confirmedPassword ){
        return newPassword.equals(confirmedPassword);
    }

    private boolean isNewPasswordDifferent(String oldPassword, String newPassword){
        return !oldPassword.equals(newPassword);
    }

    private boolean isLastAdmin(User user){
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        if(isAdmin){
            long adminCount = userRepository.countAdminUser();
            return adminCount <= 1;
        }

        return false;
    }
}
