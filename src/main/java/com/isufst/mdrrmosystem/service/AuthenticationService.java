package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.request.AuthenticationRequest;
import com.isufst.mdrrmosystem.request.RegisterRequest;
import com.isufst.mdrrmosystem.response.AuthenticationResponse;

public interface AuthenticationService {

    AuthenticationResponse register(RegisterRequest registerRequest) throws Exception;

    AuthenticationResponse login(AuthenticationRequest request);
}
