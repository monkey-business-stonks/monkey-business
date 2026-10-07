package domain.entities;

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

    @Test
    void testChangePasswordUpdatesTimestamp() {
        ZonedDateTime beforeUpdate = user.getUpdatedAt();
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        
        user.changePassword("newpassword");
        ZonedDateTime afterUpdate = user.getUpdatedAt();
        
        assertTrue(afterUpdate.isAfter(beforeUpdate), "updatedAt should be updated after password change");
    }

    @Test
    void testAccessLevelHierarchy() {
        // USER should not pass ANALYST check (ordinal: USER=0, ANALYST=1, OPERATIONS=2)
        User analystUser = new User(
            UUID.randomUUID(), "analyst", "analyst@test.com", "pass",
            "Analyst User", "555-5678", LocalDate.of(1990, 1, 1),
            User.AccessLevel.ANALYST, new HashSet<>(), now, now
        );
        
        assertTrue(analystUser.checkAccessLevel(User.AccessLevel.USER), "ANALYST should have USER level access");
        assertFalse(user.checkAccessLevel(User.AccessLevel.ANALYST), "USER should not have ANALYST level access");
    }

    @Test
    void testAddDuplicateAccount() {
        UUID accountId = UUID.randomUUID();
        Account testAccount = new Account(
            accountId, ZonedDateTime.now(), Account.AccountType.BROKERAGE,
            new BigDecimal("10000.00"), new BigDecimal("10000.00"),
            new HashSet<>(), new HashSet<>()
        );
        
        user.addAccount(testAccount);
        user.addAccount(testAccount);
        
        // HashSet behavior: duplicates should not increase size
        long count = user.getAccounts().stream().filter(a -> a.getAccID().equals(accountId)).count();
        assertEquals(1, count, "Duplicate accounts should not be added twice");
    }

    @Test
    void testOperationsAccessLevel() {
        User opsUser = new User(
            UUID.randomUUID(), "ops", "ops@test.com", "pass",
            "Ops User", "555-9999", LocalDate.of(1990, 1, 1),
            User.AccessLevel.OPERATIONS, new HashSet<>(), now, now
        );
        
        assertTrue(opsUser.checkAccessLevel(User.AccessLevel.USER), "OPERATIONS should have USER access");
        assertTrue(opsUser.checkAccessLevel(User.AccessLevel.ANALYST), "OPERATIONS should have ANALYST access");
        assertTrue(opsUser.checkAccessLevel(User.AccessLevel.OPERATIONS), "OPERATIONS should have OPERATIONS access");
    }
}
