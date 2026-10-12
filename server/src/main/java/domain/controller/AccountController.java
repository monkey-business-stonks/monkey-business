package domain.controller;

import domain.dto.*;
import domain.error.AuthenticationException;
import domain.service.AccountService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/users/{userId}/accounts")
@CrossOrigin(origins = "*")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    @Autowired
    private AccountService accountService;

    @PostMapping
    @PreAuthorize("@securityService.isUserOwner(authentication, T(java.util.UUID).fromString(#userId)) or hasAuthority('ROLE_OPERATIONS')")
    public ResponseEntity<AccountResponse> createAccount(
            @PathVariable String userId,
            @Valid @RequestBody CreateAccountRequest request) {
        UUID id = UUID.fromString(userId);
        logger.info("Creating account for user: {}", userId);
        AccountResponse account = accountService.createAccount(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    /**
     * List all accounts for a user
     */
    @GetMapping
    @PreAuthorize("@securityService.isUserOwner(authentication, T(java.util.UUID).fromString(#userId)) or hasAuthority('ROLE_OPERATIONS')")
    public ResponseEntity<List<AccountResponse>> listAccounts(@PathVariable String userId) {
        UUID id = UUID.fromString(userId);
        logger.debug("Listing accounts for user: {}", userId);
        List<AccountResponse> accounts = accountService.listAccounts(id);
        return ResponseEntity.ok(accounts);
    }

    @PostMapping("/verify-type")
    @PreAuthorize("#userId == authentication.name or hasAuthority('ROLE_OPERATIONS')")
    public ResponseEntity<Map<String, Object>> verifyAccountType(
            @PathVariable String userId,
            @Valid @RequestBody VerifyTypeRequest request) {
        UUID id = UUID.fromString(userId);

        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            throw new AuthenticationException("Refresh token is required");
        }

        accountService.verifyAccountType(id, request.getRefreshToken(), request.getAccountType());

        Map<String, Object> response = new HashMap<>();
        response.put("verified", true);
        response.put("accountType", request.getAccountType());
        return ResponseEntity.ok(response);
    }

    public static class VerifyTypeRequest {
        private String refreshToken;
        private String accountType;

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public String getAccountType() {
            return accountType;
        }

        public void setAccountType(String accountType) {
            this.accountType = accountType;
        }
    }
}
