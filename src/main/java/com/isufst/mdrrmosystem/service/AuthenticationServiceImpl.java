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
import java.util.Optional;

@Service
public class AuthenticationServiceImpl   implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthenticationServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                     AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public void register(RegisterRequest registerRequest) throws Exception {

        if(isEmailTaken(registerRequest.getEmail())){
            throw new Exception("Email already exist");
        }

        User user = buildNewUser(registerRequest);
        userRepository.save(user);
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

    private boolean isEmailTaken(String email){
       return userRepository.findByEmail(email).isPresent();
    }

    private User buildNewUser(RegisterRequest input) {
        User user = new User();

        user.setId(0);
        user.setFirstName(input.getFirstName());
        user.setMiddleName(input.getMiddleName());
        user.setLastName(input.getLastName());
        user.setNumber(input.getNumber());
        user.setEmail(input.getEmail());
        user.setPassword(passwordEncoder.encode(input.getPassword()));
        user.setAuthorities(initialAuthority());

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
