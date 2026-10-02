import domain.entities.User;
import domain.entities.Account;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

class UserTest {
    private User user;
    private UUID testUserId;
    private Set<Account> testAccounts;
    private ZonedDateTime now;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testAccounts = new HashSet<>();
        now = ZonedDateTime.now();
        user = new User(
            testUserId,
            "testuser",
            "test@example.com",
            "password123",
            "Test User",
            "555-1234",
            LocalDate.of(1990, 1, 1),
            User.AccessLevel.USER,
            testAccounts,
            now,
            now
        );
    }

    @Test
    void testUserCreation() {
        assertEquals(testUserId, user.getUserId(), "User ID should match");
        assertEquals("testuser", user.getUsername(), "Username should match");
        assertEquals("test@example.com", user.getEmail(), "Email should match");
        assertEquals(User.AccessLevel.USER, user.getAccessLevel(), "Access level should be USER");
    }

    @Test
    void testUserGettersAndSetters() {
        assertEquals("Test User", user.getName(), "Name should match");
        assertEquals("555-1234", user.getPhone(), "Phone should match");
        
        user.setName("Updated Name");
        assertEquals("Updated Name", user.getName(), "Name should be updated");
    }

    @Test
    void testChangePassword() {
        String oldHash = user.getPasswordHash();
        user.changePassword("newpassword456");
        assertNotEquals(oldHash, user.getPasswordHash(), "Password hash should change");
        assertEquals("newpassword456", user.getPasswordHash(), "Password should be updated to new value");
    }

    @Test
    void testCheckAccessLevel() {
        assertTrue(user.checkAccessLevel(User.AccessLevel.USER), "User should have USER access level");
        assertFalse(user.checkAccessLevel(User.AccessLevel.ANALYST), "User should not have ANALYST access level");
    }

    @Test
    void testAddAccount() {
        UUID accountId = UUID.randomUUID();
        Account testAccount = new Account(
            accountId,
            ZonedDateTime.now(),
            Account.AccountType.BROKERAGE,
            new BigDecimal("10000.00"),
            new BigDecimal("5000.00"),
            new HashSet<>(),
            new HashSet<>()
        );
        user.addAccount(testAccount);
        assertTrue(user.getAccounts().contains(testAccount), "Account should be added to user");
    }
}
