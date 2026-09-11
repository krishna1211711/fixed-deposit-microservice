package com.bank.fd.service.impl;

import com.bank.fd.config.JwtTokenProvider;
import com.bank.fd.dto.request.LoginRequest;
import com.bank.fd.dto.request.RegisterRequest;
import com.bank.fd.dto.response.ApiResponse;
import com.bank.fd.dto.response.AuthResponse;
import com.bank.fd.entity.CustomerProfile;
import com.bank.fd.entity.User;
import com.bank.fd.repository.CustomerProfileRepository;
import com.bank.fd.repository.UserRepository;
import com.bank.fd.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Auth service handling registration and login.
 * UserDetailsService has been extracted to CustomUserDetailsService
 * to eliminate the circular dependency with AuthenticationManager.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthServiceImpl(UserRepository userRepository,
                           CustomerProfileRepository customerProfileRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public ApiResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username is already taken!");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRole(request.getRole() != null ? request.getRole() : "CUSTOMER");

        userRepository.save(user);

        return ApiResponse.success("User registered successfully");
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Resolve the DB user to get their ID for customer profile lookup
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + request.getUsername()));

        // Look up the customer profile to obtain the customerId — may be null for ADMIN/BANK_OFFICER
        String customerId = null;
        Optional<CustomerProfile> profileOpt = customerProfileRepository.findByUserId(user.getId());
        if (profileOpt.isPresent()) {
            customerId = profileOpt.get().getCustomerId();
        }

        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .orElse("ROLE_CUSTOMER");

        String jwt = tokenProvider.generateToken(authentication, customerId);

        AuthResponse response = new AuthResponse(jwt, "Bearer", request.getUsername(), role);
        response.setCustomerId(customerId);
        return response;
    }
}
