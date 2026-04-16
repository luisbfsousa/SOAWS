package com.ecommerce.service;

import com.ecommerce.model.User;
import com.ecommerce.model.UserRole;
import com.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private User newCustomer;

    @BeforeEach
    void setUp() {
        newCustomer = User.builder()
                .id(1L)
                .username("testcustomer")
                .password(BCrypt.hashpw("password123", BCrypt.gensalt()))
                .role(UserRole.CUSTOMER)
                .build();
    }

    @Test
    void register_success() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = authService.register("newuser", "password123", UserRole.CUSTOMER);

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals(UserRole.CUSTOMER, result.getRole());
        assertTrue(BCrypt.checkpw("password123", result.getPassword()));

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_failWhenUsernameExists() {
        when(userRepository.findByUsername("testcustomer")).thenReturn(Optional.of(newCustomer));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authService.register("testcustomer", "password123", UserRole.CUSTOMER)
        );

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_success() {
        when(userRepository.findByUsername("testcustomer")).thenReturn(Optional.of(newCustomer));

        User result = authService.login("testcustomer", "password123");

        assertNotNull(result);
        assertEquals("testcustomer", result.getUsername());
    }

    @Test
    void login_failWithInvalidPassword() {
        when(userRepository.findByUsername("testcustomer")).thenReturn(Optional.of(newCustomer));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authService.login("testcustomer", "wrongpassword")
        );

        assertEquals("Invalid password", exception.getMessage());
    }

    @Test
    void login_failWhenUserNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authService.login("nonexistent", "password123")
        );

        assertEquals("User not found", exception.getMessage());
    }
}
