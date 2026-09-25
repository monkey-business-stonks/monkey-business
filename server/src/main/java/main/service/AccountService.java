package main.service;

import main.Account;
import main.dto.AccountResponse;
import main.dto.CreateAccountRequest;
import main.entity.AccountEntity;
import main.entity.UserEntity;
import main.repository.AccountRepository;
import main.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Create account for user
     */
    public AccountResponse createAccount(UUID userId, CreateAccountRequest request) {
        // Verify user exists
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + userId));

        // Validate request
        if (request.getAccountType() == null || request.getAccountType().isEmpty()) {
            throw new IllegalArgumentException("Account type is required");
        }

        // Validate account type
        try {
            AccountEntity.AccountType.valueOf(request.getAccountType().toUpperCase());
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

        // Create account entity
        UUID accountId = UUID.randomUUID();
        AccountEntity account = new AccountEntity(
            accountId,
            user,
            AccountEntity.AccountType.valueOf(request.getAccountType().toUpperCase()),
            balance,
            cashBalance,
            ZonedDateTime.now()
        );

        // Save to database
        account = accountRepository.save(account);
        return toAccountResponse(account);
    }

    /**
     * Get account by ID
     */
    public AccountResponse getAccount(UUID accountId) {
        AccountEntity account = accountRepository.findById(accountId)
            .orElseThrow(() -> new NoSuchElementException("Account not found with ID: " + accountId));
        return toAccountResponse(account);
    }

    /**
     * List all accounts for a user
     */
    public List<AccountResponse> listAccounts(UUID userId) {
        // Verify user exists
        userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + userId));

        // Get all accounts for user
        return accountRepository.findByUserUserId(userId).stream()
            .map(this::toAccountResponse)
            .toList();
    }

    /**
     * Get stored account (internal use)
     */
    public AccountEntity getAccountDirect(UUID accountId) {
        return accountRepository.findById(accountId).orElse(null);
    }

    /**
     * Save account (internal use for transaction updates)
     */
    public AccountEntity saveAccount(AccountEntity account) {
        return accountRepository.save(account);
    }

    /**
     * Convert AccountEntity to AccountResponse
     */
    private AccountResponse toAccountResponse(AccountEntity account) {
        return new AccountResponse(
            account.getAccountId(),
            account.getUser().getUserId(),
            account.getAccountType().toString(),
            account.getOpenedDate(),
            account.getBalance(),
            account.getCashBalance()
        );
    }
}
