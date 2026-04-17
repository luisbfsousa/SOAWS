package com.ecommerce.controller;

import com.ecommerce.datatransfer.AuthRequest;
import com.ecommerce.datatransfer.RegisterRequest;
import com.ecommerce.model.User;
import com.ecommerce.model.UserRole;
import com.ecommerce.service.AuthService;
import com.ecommerce.service.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .username("real user")
                .role(UserRole.CUSTOMER)
                .build();
    }

    @Test
    void register_success() throws Exception {
        RegisterRequest request = new RegisterRequest("real user", "password123", UserRole.CUSTOMER);

        when(authService.register(anyString(), anyString(), any(UserRole.class))).thenReturn(mockUser);
        when(jwtUtil.generateToken(mockUser)).thenReturn("totally-real-token");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("real user"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.token").value("totally-real-token"))
                .andExpect(jsonPath("$.message").value("User registered successfully"));

        verify(authService).register(anyString(), anyString(), any(UserRole.class));
        verify(jwtUtil).generateToken(mockUser);
    }

    @Test
    void register_failure_usernameExists() throws Exception {
        RegisterRequest request = new RegisterRequest("real user", "password123", UserRole.CUSTOMER);

        when(authService.register(anyString(), anyString(), any(UserRole.class)))
                .thenThrow(new IllegalArgumentException("Username already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username already exists"));

        verify(authService).register(anyString(), anyString(), any(UserRole.class));
    }

    @Test
    void login_success() throws Exception {
        AuthRequest request = new AuthRequest("real user", "password123");

        when(authService.login("real user", "password123")).thenReturn(mockUser);
        when(jwtUtil.generateToken(mockUser)).thenReturn("totally-real-token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("real user"))
                .andExpect(jsonPath("$.token").value("totally-real-token"))
                .andExpect(jsonPath("$.message").value("Login successful"));

        verify(authService).login("real user", "password123");
        verify(jwtUtil).generateToken(mockUser);
    }

    @Test
    void login_failure_invalidCredentials() throws Exception {
        AuthRequest request = new AuthRequest("real user", "wrongpassword");

        when(authService.login("real user", "wrongpassword"))
                .thenThrow(new IllegalArgumentException("Invalid password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid password"));

        verify(authService).login("real user", "wrongpassword");
    }
}
