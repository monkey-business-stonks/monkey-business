package main.service;

import main.Account;
import main.dto.AccountResponse;
import main.dto.CreateAccountRequest;
import main.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class AccountService {

    @Autowired
    private UserService userService;

    // In-memory storage for demo
    private final Map<UUID, Account> accountDatabase = new HashMap<>();
    private final Map<UUID, List<UUID>> userAccountIndex = new HashMap<>();

    /**
     * Create account for user
     */
    public AccountResponse createAccount(UUID userId, CreateAccountRequest request) {
        // Verify user exists
        User user = userService.getUserDirect(userId);
        if (user == null) {
            throw new NoSuchElementException("User not found with ID: " + userId);
        }

        // Validate request
        if (request.getAccountType() == null) {
            throw new IllegalArgumentException("Account type is required");
        }

        // Validate account type
        try {
            Account.AccountType.valueOf(request.getAccountType().toString());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid account type: " + request.getAccountType());
        }

        // Set default balances if not provided
        BigDecimal balance = request.getInitialBalance() != null 
            ? request.getInitialBalance() 
            : BigDecimal.ZERO;
        BigDecimal cashBalance = request.getInitialCashBalance() != null 
            ? request.getInitialCashBalance() 
            : balance;  // Default: all balance is cash

        // Create account
        UUID accountId = UUID.randomUUID();
        Account account = new Account(
            accountId,
            ZonedDateTime.now(),
            Account.AccountType.valueOf(request.getAccountType().toString()),
            balance,
            cashBalance,
            new HashSet<>(),
            new HashSet<>()
        );

        // Store account
        accountDatabase.put(accountId, account);
        userAccountIndex.computeIfAbsent(userId, k -> new ArrayList<>()).add(accountId);

        return toAccountResponse(account, userId);
    }

    /**
     * Get account by ID
     */
    public AccountResponse getAccount(UUID accountId) {
        Account account = accountDatabase.get(accountId);
        if (account == null) {
            throw new NoSuchElementException("Account not found with ID: " + accountId);
        }
        // Find user ID (in production, would be from account metadata)
        UUID userId = userAccountIndex.entrySet().stream()
            .filter(e -> e.getValue().contains(accountId))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("User not found for account: " + accountId));

        return toAccountResponse(account, userId);
    }

    /**
     * List all accounts for a user
     */
    public List<AccountResponse> listAccounts(UUID userId) {
        // Verify user exists
        User user = userService.getUserDirect(userId);
        if (user == null) {
            throw new NoSuchElementException("User not found with ID: " + userId);
        }

        List<UUID> accountIds = userAccountIndex.getOrDefault(userId, new ArrayList<>());
        return accountIds.stream()
            .map(accountDatabase::get)
            .map(account -> toAccountResponse(account, userId))
            .toList();
    }

    /**
     * Get stored account (internal use)
     */
    public Account getAccountDirect(UUID accountId) {
        return accountDatabase.get(accountId);
    }

    /**
     * Convert Account to AccountResponse
     */
    private AccountResponse toAccountResponse(Account account, UUID userId) {
        // Convert domain enum to DTO enum (replace underscores with spaces for ROTH_IRA)
        String accountTypeStr = account.getAccType().toString().replace("_", " ");
        
        return new AccountResponse()
            .accountId(account.getAccID())
            .userId(userId)
            .accountType(main.dto.AccountType.fromValue(accountTypeStr))
            .openedDate(account.getOpenDate().toOffsetDateTime())
            .balance(new BigDecimal(account.getBalance()))  // Convert double to BigDecimal
            .cashBalance(account.getCashBalance());
    }
}
