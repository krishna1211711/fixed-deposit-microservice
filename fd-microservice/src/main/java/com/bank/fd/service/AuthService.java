package com.bank.fd.service;

import com.bank.fd.dto.request.LoginRequest;
import com.bank.fd.dto.request.RegisterRequest;
import com.bank.fd.dto.response.ApiResponse;
import com.bank.fd.dto.response.AuthResponse;

public interface AuthService {
    ApiResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
