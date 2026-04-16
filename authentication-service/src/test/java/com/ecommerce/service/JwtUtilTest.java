package com.ecommerce.service;

import com.ecommerce.model.User;
import com.ecommerce.model.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        jwtUtil.init();
    }

    @Test
    void generateToken_createsValidToken() {
        User user = User.builder()
                .id(1L)
                .username("real user")
                .role(UserRole.CUSTOMER)
                .build();

        String token = jwtUtil.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }
}
