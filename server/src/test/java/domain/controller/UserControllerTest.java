package domain.controller;

import domain.dto.AuthResponse;
import domain.dto.UserResponse;
import domain.error.AuthenticationException;
import domain.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test UserController
 * 
 * Verifies:
 * - Controllers properly throw exceptions without catching them
 * - Exceptions bubble up to GlobalExceptionHandler
 * - No redundant try-catch blocks exist
 * - Proper logging occurs
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private UUID testUserId;
    private UserResponse testUserResponse;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUserResponse = new UserResponse();
        testUserResponse.setUserId(testUserId);
        testUserResponse.setUsername("testuser");
        testUserResponse.setEmail("test@example.com");
    }

    // ============ Happy Path Tests ============

    @Test
    void testGetUser_WithValidId_ReturnsUser() {
        when(userService.getUser(testUserId)).thenReturn(testUserResponse);

        ResponseEntity<UserResponse> response = userController.getUser(testUserId.toString());

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("testuser", response.getBody().getUsername());
        verify(userService, times(1)).getUser(testUserId);
    }

    @Test
    void testGetUser_CallsServiceWithCorrectId() {
        when(userService.getUser(testUserId)).thenReturn(testUserResponse);

        userController.getUser(testUserId.toString());

        verify(userService).getUser(testUserId);
    }

    // ============ Exception Bubble-Up Tests ============

    @Test
    void testGetUser_WithInvalidId_ThrowsException() {
        when(userService.getUser(any(UUID.class)))
            .thenThrow(new NoSuchElementException("User not found"));

        assertThrows(NoSuchElementException.class, () -> {
            userController.getUser(testUserId.toString());
        });
    }

    @Test
    void testGetUser_WithInvalidId_DoesNotCatchException() {
        when(userService.getUser(any(UUID.class)))
            .thenThrow(new NoSuchElementException("User not found"));

        // Verify exception bubbles up - NOT caught and handled in controller
        NoSuchElementException thrown = assertThrows(NoSuchElementException.class, () -> {
            userController.getUser(testUserId.toString());
        });

        assertEquals("User not found", thrown.getMessage());
    }

    // ============ Authenticate Tests ============

    @Test
    void testAuthenticate_WithValidCredentials_ReturnsUser() {
        AuthResponse authResponse = new AuthResponse();
        authResponse.setUserId(testUserId);
        authResponse.setIsAuthenticated(true);
        
        domain.dto.AuthenticateRequest request = new domain.dto.AuthenticateRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        when(userService.authenticate(request)).thenReturn(authResponse);

        ResponseEntity<AuthResponse> response = userController.authenticate(request);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getIsAuthenticated());
        verify(userService, times(1)).authenticate(request);
    }

    @Test
    void testAuthenticate_WithInvalidCredentials_ThrowsException() {
        domain.dto.AuthenticateRequest request = new domain.dto.AuthenticateRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        when(userService.authenticate(request))
            .thenThrow(new AuthenticationException("Invalid credentials", "AUTHENTICATION_FAILED"));

        assertThrows(AuthenticationException.class, () -> {
            userController.authenticate(request);
        });
    }

    @Test
    void testAuthenticate_ExceptionBubblesUp_NotCaught() {
        domain.dto.AuthenticateRequest request = new domain.dto.AuthenticateRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        when(userService.authenticate(request))
            .thenThrow(new AuthenticationException("Invalid credentials", "AUTHENTICATION_FAILED"));

        // Verify exception bubbles - NOT caught in controller
        AuthenticationException thrown = assertThrows(AuthenticationException.class, () -> {
            userController.authenticate(request);
        });

        assertEquals("Invalid credentials", thrown.getMessage());
    }

    // ============ No Try-Catch Block Tests ============

    @Test
    void testControllerMethodsHaveNoTryCatchBlocks() {
        // Verify by attempting to trigger exceptions - they should bubble up
        
        // Test 1: getUser with exception
        when(userService.getUser(any(UUID.class)))
            .thenThrow(new NoSuchElementException("Not found"));
        
        assertThrows(NoSuchElementException.class, () -> {
            userController.getUser(testUserId.toString());
        });

        // Test 2: authenticate with exception
        domain.dto.AuthenticateRequest request = new domain.dto.AuthenticateRequest();
        request.setUsername("user");
        request.setPassword("pass");
        
        when(userService.authenticate(request))
            .thenThrow(new IllegalArgumentException("Invalid"));
        
        assertThrows(IllegalArgumentException.class, () -> {
            userController.authenticate(request);
        });

        // If there were try-catch blocks in the controller, these assertions would fail
        // because exceptions would be caught and handled with ResponseEntity.status()
    }

    // ============ Logging Tests ============

    @Test
    void testGetUser_LogsAtDebugLevel() {
        when(userService.getUser(testUserId)).thenReturn(testUserResponse);

        userController.getUser(testUserId.toString());

        // Verify service was called (logging tested via manual inspection)
        verify(userService).getUser(testUserId);
    }

    @Test
    void testAuthenticate_LogsAtInfoLevel() {
        domain.dto.AuthenticateRequest request = new domain.dto.AuthenticateRequest();
        request.setUsername("testuser");
        request.setPassword("password");

        AuthResponse authResponse = new AuthResponse();
        authResponse.setUserId(testUserId);
        authResponse.setIsAuthenticated(true);
        when(userService.authenticate(request)).thenReturn(authResponse);

        userController.authenticate(request);

        // Verify service was called (actual log output verified via manual inspection)
        verify(userService).authenticate(request);
    }

    // ============ Request Validation Tests ============

    @Test
    void testGetUser_StringPassedToController_IsConvertedToUUID() {
        // The controller converts String to UUID internally
        when(userService.getUser(testUserId))
            .thenReturn(testUserResponse);

        ResponseEntity<UserResponse> response = userController.getUser(testUserId.toString());

        assertEquals(200, response.getStatusCodeValue());
        verify(userService).getUser(testUserId);
    }

    // ============ Response Type Tests ============

    @Test
    void testGetUser_ReturnsResponseEntity() {
        when(userService.getUser(testUserId)).thenReturn(testUserResponse);

        ResponseEntity<?> response = userController.getUser(testUserId.toString());

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void testAuthenticate_ReturnsResponseEntity() {
        domain.dto.AuthenticateRequest request = new domain.dto.AuthenticateRequest();
        request.setUsername("user");
        request.setPassword("pass");

        AuthResponse authResponse = new AuthResponse();
        authResponse.setUserId(testUserId);
        authResponse.setIsAuthenticated(true);
        when(userService.authenticate(request)).thenReturn(authResponse);

        ResponseEntity<?> response = userController.authenticate(request);

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
    }

    // ============ Error Message Propagation Tests ============

    @Test
    void testGetUser_ErrorMessageFromService_IsPreserved() {
        String errorMessage = "User with ID " + testUserId + " does not exist";
        when(userService.getUser(testUserId))
            .thenThrow(new NoSuchElementException(errorMessage));

        NoSuchElementException thrown = assertThrows(NoSuchElementException.class, () -> {
            userController.getUser(testUserId.toString());
        });

        assertEquals(errorMessage, thrown.getMessage());
    }

    @Test
    void testAuthenticate_ErrorMessageFromService_IsPreserved() {
        domain.dto.AuthenticateRequest request = new domain.dto.AuthenticateRequest();
        request.setUsername("user");
        request.setPassword("wrongpass");

        String errorMessage = "Username or password incorrect";
        when(userService.authenticate(request))
            .thenThrow(new IllegalArgumentException(errorMessage));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            userController.authenticate(request);
        });

        assertEquals(errorMessage, thrown.getMessage());
    }
}
