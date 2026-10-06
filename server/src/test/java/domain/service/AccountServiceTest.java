package domain.service;

import domain.dto.AccountResponse;
import domain.dto.CreateAccountRequest;
import domain.entities.Account;
import domain.entities.User;
import domain.repository.AccountRepository;
import domain.repository.AssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private AccountService accountService;

    private UUID testUserId;
    private UUID testAccountId;
    private User testUser;
    private Account testAccount;
    private CreateAccountRequest createAccountRequest;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testAccountId = UUID.randomUUID();
        
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
        
        testAccount = new Account(
            testAccountId,
            ZonedDateTime.now(),
            Account.AccountType.BROKERAGE,
            BigDecimal.valueOf(10000),
            BigDecimal.valueOf(10000),
            new HashSet<>(),
            new HashSet<>()
        );
        testAccount.setUser(testUser);
        
        createAccountRequest = new CreateAccountRequest();
        createAccountRequest.setAccountType(domain.dto.AccountType.BROKERAGE);
    }

    @Test
    void testCreateAccount_Success() {
        // Arrange
        when(userService.getUserDirect(testUserId)).thenReturn(testUser);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(assetRepository.findAssetsByAccountIdOrderByValue(any())).thenReturn(new ArrayList<>());

        // Act
        AccountResponse response = accountService.createAccount(testUserId, createAccountRequest);

        // Assert
        assertNotNull(response);
        assertEquals(testUserId, response.getUserId());
        verify(userService, times(1)).getUserDirect(testUserId);
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void testCreateAccount_UserNotFound() {
        // Arrange
        when(userService.getUserDirect(testUserId)).thenReturn(null);

        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> accountService.createAccount(testUserId, createAccountRequest));
    }

    @Test
    void testCreateAccount_MissingAccountType() {
        // Arrange
        createAccountRequest.setAccountType(null);
        when(userService.getUserDirect(testUserId)).thenReturn(testUser);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> accountService.createAccount(testUserId, createAccountRequest));
    }

    @Test
    void testGetAccount_Success() {
        // Arrange
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(assetRepository.findAssetsByAccountIdOrderByValue(any())).thenReturn(new ArrayList<>());

        // Act
        AccountResponse response = accountService.getAccount(testAccountId);

        // Assert
        assertNotNull(response);
        assertEquals(testUserId, response.getUserId());
        verify(accountRepository, times(1)).findById(testAccountId);
    }

    @Test
    void testGetAccount_NotFound() {
        // Arrange
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> accountService.getAccount(testAccountId));
    }

    @Test
    void testListAccounts_Success() {
        // Arrange
        List<Account> accounts = Arrays.asList(testAccount);
        when(userService.getUserDirect(testUserId)).thenReturn(testUser);
        when(accountRepository.findByUserUserId(testUserId)).thenReturn(accounts);
        when(assetRepository.findAssetsByAccountIdOrderByValue(any())).thenReturn(new ArrayList<>());

        // Act
        List<AccountResponse> responses = accountService.listAccounts(testUserId);

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        verify(accountRepository, times(1)).findByUserUserId(testUserId);
    }

    @Test
    void testListAccounts_UserNotFound() {
        // Arrange
        when(userService.getUserDirect(testUserId)).thenReturn(null);

        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> accountService.listAccounts(testUserId));
    }

    @Test
    void testListAccounts_EmptyList() {
        // Arrange
        when(userService.getUserDirect(testUserId)).thenReturn(testUser);
        when(accountRepository.findByUserUserId(testUserId)).thenReturn(new ArrayList<>());

        // Act
        List<AccountResponse> responses = accountService.listAccounts(testUserId);

        // Assert
        assertNotNull(responses);
        assertEquals(0, responses.size());
    }

    @Test
    void testGetAccountDirect_Success() {
        // Arrange
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));

        // Act
        Account account = accountService.getAccountDirect(testAccountId);

        // Assert
        assertNotNull(account);
        assertEquals(testAccountId, account.getAccountId());
    }

    @Test
    void testGetAccountDirect_NotFound() {
        // Arrange
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.empty());

        // Act
        Account account = accountService.getAccountDirect(testAccountId);

        // Assert
        assertNull(account);
    }

    @Test
    void testSaveAccount_Success() {
        // Arrange
        when(accountRepository.save(testAccount)).thenReturn(testAccount);

        // Act
        accountService.saveAccount(testAccount);

        // Assert
        verify(accountRepository, times(1)).save(testAccount);
    }
}
