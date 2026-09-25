package main;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

class UserTest {
    private User user;
    private UUID testUserId;
    private Set<Account> testAccounts;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testAccounts = new HashSet<>();
        user = new User(
            testUserId,
            "testuser",
            "test@example.com",
            "password123",
            User.AccessLevel.USER,
            true,
            testAccounts,
            LocalDateTime.now()
        );
    }

    @Test
    void testLoginWithCorrectPassword() {
        assertTrue(user.login("password123"), "Login should succeed with correct password");
    }

    @Test
    void testLoginWithIncorrectPassword() {
        assertFalse(user.login("wrongpassword"), "Login should fail with incorrect password");
    }

    @Test
    void testGetIsActive() {
        assertTrue(user.isActive(), "User should be active");
    }

    @Test
    void testLoginWhenInactive() {
        User inactiveUser = new User(
            testUserId,
            "testuser",
            "test@example.com",
            "password123",
            User.AccessLevel.USER,
            false,
            testAccounts,
            LocalDateTime.now()
        );
        assertFalse(inactiveUser.login("password123"), "Login should fail when user is inactive");
    }

    @Test
    void testChangePassword() {
        user.changePassword("password123", "newpassword456");
        assertTrue(user.login("newpassword456"), "Should login with new password");
    }

    @Test
    void testChangePasswordWithWrongOldPassword() {
        assertThrows(IllegalArgumentException.class, () -> {
            user.changePassword("wrongpassword", "newpassword456");
        }, "Should throw exception with wrong old password");
    }

    @Test
    void testCheckAccessLevel() {
        assertTrue(user.checkAccessLevel(User.AccessLevel.USER), "User should have USER access level");
        assertFalse(user.checkAccessLevel(User.AccessLevel.ANALYST), "User should not have ANALYST access level");
    }
}
