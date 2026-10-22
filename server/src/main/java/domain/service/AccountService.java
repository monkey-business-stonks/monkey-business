package domain.service;

import domain.entities.Account;
import domain.dto.AccountResponse;
import domain.dto.CreateAccountRequest;
import domain.error.AuthenticationException;
import domain.entities.User;
import domain.repository.AccountRepository;
import domain.repository.AssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Autowired
    private PricingEngine pricingEngine;

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

        if (request.getInitialDeposit() == null) {
            throw new IllegalArgumentException("Initial deposit is required");
        }

        if (request.getInitialDeposit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Initial deposit must be greater than 0");
        }

        BigDecimal maximumContribution = getMaximumContribution(request.getAccountType().toString(), user.getDob());
        if (request.getInitialDeposit().compareTo(maximumContribution) > 0) {
            throw new IllegalArgumentException("Initial deposit cannot exceed " + maximumContribution.toPlainString());
        }

        if (request.getTradingExperience() == null || request.getTradingExperience().isBlank()
                || request.getInvestmentObjective() == null || request.getInvestmentObjective().isBlank()
                || request.getRiskTolerance() == null || request.getRiskTolerance().isBlank()) {
            throw new IllegalArgumentException("Investor profile fields are required");
        }

        if (!Boolean.TRUE.equals(request.getAcceptsDisclosure())) {
            throw new IllegalArgumentException("Regulatory disclosure must be accepted");
        }

        BigDecimal cashBalance = request.getInitialDeposit();
        BigDecimal balance = cashBalance;  // Initially, all balance is cash (no holdings yet)

        // Create account
        UUID accountId = UUID.randomUUID();
        Account account = new Account(
            accountId,
            ZonedDateTime.now(),
            toEntityAccountType(request.getAccountType().toString()),
            balance,
            cashBalance,
            new HashSet<>(),
            new HashSet<>()
        );
        account.setUser(user);
        account.setTradingExperience(request.getTradingExperience());
        account.setInvestmentObjective(request.getInvestmentObjective());
        account.setRiskTolerance(request.getRiskTolerance());

        // Store account in repository
        accountRepository.save(account);

        return toAccountResponse(account, userId);
    }

    private BigDecimal getMaximumContribution(String accountType, LocalDate dateOfBirth) {
        if ("ROTH IRA".equals(accountType) || "ROTH_IRA".equals(accountType)) {
            return dateOfBirth != null && getAge(dateOfBirth) >= 50
                ? new BigDecimal("8600")
                : new BigDecimal("7500");
        }

        if ("401K".equals(accountType) || "_401K".equals(accountType)) {
            int age = dateOfBirth == null ? 0 : getAge(dateOfBirth);
            if (age >= 60 && age <= 63) return new BigDecimal("35750");
            if (age >= 50) return new BigDecimal("32500");
            return new BigDecimal("24500");
        }

        return new BigDecimal("250000");
    }

    private int getAge(LocalDate dateOfBirth) {
        return dateOfBirth.until(LocalDate.now()).getYears();
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
     * Validate account type request for authenticated user.
     * This method performs validation only and does not create or mutate accounts.
     */
    public void verifyAccountType(UUID userId, String refreshToken, String accountType) {
        User user = userService.getUserDirect(userId);
        if (user == null) {
            throw new NoSuchElementException("User not found with ID: " + userId);
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthenticationException("Refresh token is required");
        }

        if (user.getRefreshToken() == null || !refreshToken.equals(user.getRefreshToken())) {
            throw new AuthenticationException("Refresh token does not match user");
        }

        if (accountType == null || accountType.isBlank()) {
            throw new IllegalArgumentException("Account type is required");
        }

        try {
            toEntityAccountType(accountType);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid account type: " + accountType);
        }
    }

    private Account.AccountType toEntityAccountType(String accountType) {
        return switch (accountType.trim().toUpperCase()) {
            case "401K", "_401K" -> Account.AccountType._401K;
            case "ROTH IRA", "ROTH_IRA" -> Account.AccountType.ROTH_IRA;
            default -> Account.AccountType.valueOf(accountType.trim().toUpperCase());
        };
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
            BigDecimal currentPrice = pricingEngine.getPrice(asset.ticker());
            heldAssets.add(new domain.dto.AssetResponse()
                .assetId(asset.getAssetId())
                .assetClass(domain.dto.AssetClass.valueOf(asset.getAssetClass()))
                .ticker(asset.ticker())
                .name(asset.getName())
                .quantity(new BigDecimal(asset.quantity().toString()))
                .boughtAverage(asset.averageCost())
                .currentPrice(currentPrice)
            );
        }

        return new AccountResponse()
            .accountId(account.getAccID())
            .userId(userId)
            .accountType(toDtoAccountType(account.getAccType()))
            .openedDate(account.getCreatedOn().toOffsetDateTime())
            .balance(new BigDecimal(account.getBalance()))
            .cashBalance(account.getCashBalance())
            .tradingExperience(account.getTradingExperience())
            .investmentObjective(account.getInvestmentObjective())
            .riskTolerance(account.getRiskTolerance())
            .heldAssets(heldAssets);
    }

    private domain.dto.AccountType toDtoAccountType(Account.AccountType accountType) {
        return switch (accountType) {
            case _401K -> domain.dto.AccountType.fromValue("401K");
            case ROTH_IRA -> domain.dto.AccountType.fromValue("ROTH IRA");
            default -> domain.dto.AccountType.fromValue(accountType.name());
        };
    }
}
