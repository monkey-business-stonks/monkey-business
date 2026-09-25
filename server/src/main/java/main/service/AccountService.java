package main.service;

import main.Account;
import main.dto.AccountResponse;
import main.dto.CreateAccountRequest;
import main.User;
import main.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class AccountService {

    @Autowired
    private UserService userService;

    @Autowired
    private AccountRepository accountRepository;

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
        if (request.getAccountType() == null || request.getAccountType().isEmpty()) {
            throw new IllegalArgumentException("Account type is required");
        }

        // Validate account type
        try {
            Account.AccountType.valueOf(request.getAccountType().toUpperCase());
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
            Account.AccountType.valueOf(request.getAccountType().toUpperCase()),
            balance,
            cashBalance,
            new HashSet<>(),
            new HashSet<>()
        );
        account.setUser(user);

        // Store account in repository
        accountRepository.save(account);

        return toAccountResponse(account, userId);
    }

    /**
     * Get account by ID
     */
    public AccountResponse getAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new NoSuchElementException("Account not found with ID: " + accountId));
        
        UUID userId = account.getUser() != null ? account.getUser().getUserId() : null;
        if (userId == null) {
            throw new NoSuchElementException("User not found for account: " + accountId);
        }

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

        return accountRepository.findByUserUserId(userId).stream()
            .map(account -> toAccountResponse(account, userId))
            .toList();
    }

    /**
     * Get stored account (internal use)
     */
    public Account getAccountDirect(UUID accountId) {
        return accountRepository.findById(accountId).orElse(null);
    }

    /**
     * Save account to repository (for updates)
     */
    public void saveAccount(Account account) {
        accountRepository.save(account);
    }

    /**
     * Convert Account to AccountResponse
     */
    private AccountResponse toAccountResponse(Account account, UUID userId) {
        return new AccountResponse(
            account.getAccID(),
            userId,
            account.getAccType().toString(),
            account.getOpenDate(),
            new BigDecimal(account.getBalance()),
            account.getCashBalance()
        );
    }
}
