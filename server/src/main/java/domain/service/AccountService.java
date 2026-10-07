package domain.service;

import domain.entities.Account;
import domain.dto.AccountResponse;
import domain.dto.CreateAccountRequest;
import domain.entities.User;
import domain.repository.AccountRepository;
import domain.repository.AssetRepository;
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

    @Autowired
    private AssetRepository assetRepository;

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

        // Set default balances - accounts start with 10,000 cash
        BigDecimal cashBalance = new BigDecimal("10000");
        BigDecimal balance = cashBalance;  // Initially, all balance is cash (no holdings yet)

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
        // Load assets for this account
        List<domain.dto.AssetResponse> heldAssets = new ArrayList<>();
        for (domain.entities.Asset asset : assetRepository.findAssetsByAccountIdOrderByValue(account.getAccID())) {
            heldAssets.add(new domain.dto.AssetResponse()
                .assetId(asset.getAssetId())
                .assetClass(domain.dto.AssetClass.valueOf(asset.getAssetClass()))
                .ticker(asset.ticker())
                .name(asset.getName())
                .quantity(new BigDecimal(asset.quantity().toString()))
                .boughtAverage(asset.averageCost())
            );
        }

        return new AccountResponse()
            .accountId(account.getAccID())
            .userId(userId)
            .accountType(domain.dto.AccountType.valueOf(account.getAccType().toString()))
            .openedDate(account.getCreatedOn().toOffsetDateTime())
            .balance(new BigDecimal(account.getBalance()))
            .cashBalance(account.getCashBalance())
            .heldAssets(heldAssets);
    }
}
