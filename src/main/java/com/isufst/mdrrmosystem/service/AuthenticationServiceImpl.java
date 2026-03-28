package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Authority;
import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.repository.UserRepository;
import com.isufst.mdrrmosystem.request.AuthenticationRequest;
import com.isufst.mdrrmosystem.request.RegisterRequest;
import com.isufst.mdrrmosystem.response.AuthenticationResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthenticationServiceImpl(UserRepository userRepository,
                                     PasswordEncoder passwordEncoder,
                                     AuthenticationManager authenticationManager,
                                     JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public AuthenticationResponse register(RegisterRequest registerRequest) {
        // Corrected syntax: added missing closing parenthesis
        if (isEmailTaken(registerRequest.email())) {
            throw new RuntimeException("Email already exists");
        }

        User user = buildNewUser(registerRequest);
        User savedUser = userRepository.save(user);

        // Return token immediately after registration
        String jwtToken = jwtService.generateToken(new HashMap<>(), savedUser);
        return new AuthenticationResponse(jwtToken);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticationResponse login(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        String jwtToken = jwtService.generateToken(new HashMap<>(), user);

        return new AuthenticationResponse(jwtToken);
    }

    private boolean isEmailTaken(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    private User buildNewUser(RegisterRequest request) {
        User user = new User();
        user.setFirstName(request.firstName());
        user.setMiddleName(request.middleName());
        user.setLastName(request.lastName());
        user.setNumber(request.number());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPosition(request.position());
        user.setOffice(request.office());
        user.setAccountStatus(request.accountStatus());
        user.setResponderEligible(request.responderEligible());
        user.setCoordinatorEligible(request.coordinatorEligible());

        // Logic: Use provided authorities if present, otherwise use initial defaults
        if (request.authorities() != null && !request.authorities().isEmpty()) {
            List<Authority> authorities = request.authorities().stream()
                    .map(Authority::new)
                    .collect(Collectors.toList());
            user.setAuthorities(authorities);
        } else {
            user.setAuthorities(initialAuthority());
        }

        return user;
    }

    private List<Authority> initialAuthority() {
        boolean isFirstUser = userRepository.count() == 0;
        List<Authority> authorities = new ArrayList<>();

        authorities.add(new Authority("ROLE_USER"));
        if (isFirstUser) {
            authorities.add(new Authority("ROLE_ADMIN"));
        }
        return authorities;
    }
}