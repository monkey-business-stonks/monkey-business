package domain.service;

import domain.dto.AuthResponse;
import domain.dto.AuthenticateRequest;
import domain.dto.CreateUserRequest;
import domain.dto.UserResponse;
import domain.entities.User;
import domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private UUID testUserId;
    private User testUser;
    private CreateUserRequest createRequest;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = new User(
            testUserId,
            "testuser",
            "test@example.com",
            "password123",
            "Test User",
            null,
            LocalDate.of(2000, 1, 1),
            User.AccessLevel.USER,
            new HashSet<>(),
            ZonedDateTime.now(),
            ZonedDateTime.now()
        );

        createRequest = new CreateUserRequest();
        createRequest.setUsername("newuser");
        createRequest.setName("New User");
        createRequest.setEmail("new@example.com");
        createRequest.setPassword("password123");
        createRequest.setDob(LocalDate.of(2000, 1, 1));
    }

    @Test
    void testCreateUser_Success() {
        // Arrange
        User newUser = new User(
            UUID.randomUUID(),
            createRequest.getUsername(),
            createRequest.getEmail(),
            createRequest.getPassword(),
            createRequest.getName(),
            null,
            createRequest.getDob(),
            User.AccessLevel.USER,
            new HashSet<>(),
            ZonedDateTime.now(),
            ZonedDateTime.now()
        );
        when(userRepository.findByUsername(createRequest.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(createRequest.getEmail())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // Act
        UserResponse response = userService.createUser(createRequest);

        // Assert
        assertNotNull(response);
        assertEquals("newuser", response.getUsername());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testCreateUser_MissingUsername() {
        // Arrange
        createRequest.setUsername(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> userService.createUser(createRequest));
    }

    @Test
    void testCreateUser_MissingEmail() {
        // Arrange
        createRequest.setEmail(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> userService.createUser(createRequest));
    }

    @Test
    void testCreateUser_PasswordTooShort() {
        // Arrange
        createRequest.setPassword("short");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> userService.createUser(createRequest));
    }

    @Test
    void testCreateUser_UsernameDuplicate() {
        // Arrange
        when(userRepository.findByUsername(createRequest.getUsername())).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> userService.createUser(createRequest));
    }

    @Test
    void testCreateUser_EmailDuplicate() {
        // Arrange
        when(userRepository.findByUsername(createRequest.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(createRequest.getEmail())).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> userService.createUser(createRequest));
    }

    @Test
    void testGetUser_Success() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // Act
        UserResponse response = userService.getUser(testUserId);

        // Assert
        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("Test User", response.getName());
    }

    @Test
    void testGetUser_NotFound() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> userService.getUser(testUserId));
    }

    @Test
    void testAuthenticate_Success() {
        // Arrange
        AuthenticateRequest authRequest = new AuthenticateRequest();
        authRequest.setUsername("testuser");
        authRequest.setPassword("password123");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        AuthResponse response = userService.authenticate(authRequest);

        // Assert
        assertNotNull(response);
        assertTrue(response.getIsAuthenticated());
        assertEquals(testUserId, response.getUserId());
    }

    @Test
    void testAuthenticate_UserNotFound() {
        // Arrange
        AuthenticateRequest authRequest = new AuthenticateRequest();
        authRequest.setUsername("nonexistent");
        authRequest.setPassword("password123");

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> userService.authenticate(authRequest));
    }

    @Test
    void testAuthenticate_MissingPassword() {
        // Arrange
        AuthenticateRequest authRequest = new AuthenticateRequest();
        authRequest.setUsername("testuser");
        authRequest.setPassword(null);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> userService.authenticate(authRequest));
    }

    @Test
    void testGetUserDirect_Success() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // Act
        User user = userService.getUserDirect(testUserId);

        // Assert
        assertNotNull(user);
        assertEquals("testuser", user.username());
    }

    @Test
    void testGetUserDirect_NotFound() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act
        User user = userService.getUserDirect(testUserId);

        // Assert
        assertNull(user);
    }

    @Test
    void testGetUserByUsername_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        User user = userService.getUserByUsername("testuser");

        // Assert
        assertNotNull(user);
        assertEquals("testuser", user.username());
    }

    @Test
    void testGetUserByUsername_NotFound() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act
        User user = userService.getUserByUsername("nonexistent");

        // Assert
        assertNull(user);
    }
}
