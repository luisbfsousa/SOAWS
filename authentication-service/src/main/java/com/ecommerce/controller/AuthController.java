package com.ecommerce.controller;

import com.ecommerce.datatransfer.AuthRequest;
import com.ecommerce.datatransfer.AuthResponse;
import com.ecommerce.datatransfer.RegisterRequest;
import com.ecommerce.model.User;
import com.ecommerce.model.UserRole;
import com.ecommerce.service.AuthService;
import com.ecommerce.service.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @Autowired
    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        try {
            User user = authService.register(
                    request.getUsername(),
                    request.getPassword(),
                    UserRole.CUSTOMER);
            String token = jwtUtil.generateToken(user);
            AuthResponse response = AuthResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .role(user.getRole())
                    .message("User registered successfully")
                    .token(token)
                    .build();
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(
                    AuthResponse.builder().message(e.getMessage()).build(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        try {
            User user = authService.login(request.getUsername(), request.getPassword());
            String token = jwtUtil.generateToken(user);
            AuthResponse response = AuthResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .role(user.getRole())
                    .message("Login successful")
                    .token(token)
                    .build();
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(
                    AuthResponse.builder().message(e.getMessage()).build(),
                    HttpStatus.UNAUTHORIZED
            );
        }
    }
}
