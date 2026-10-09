package domain.controller;

import domain.dto.*;
import domain.service.AccountService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users/{userId}/accounts")
@CrossOrigin(origins = "*")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    @Autowired
    private AccountService accountService;

    @PostMapping
    @PreAuthorize("#userId == authentication.name or hasAuthority('ROLE_OPERATIONS')")
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
    @PreAuthorize("#userId == authentication.name or hasAuthority('ROLE_OPERATIONS')")
    public ResponseEntity<List<AccountResponse>> listAccounts(@PathVariable String userId) {
        UUID id = UUID.fromString(userId);
        logger.debug("Listing accounts for user: {}", userId);
        List<AccountResponse> accounts = accountService.listAccounts(id);
        return ResponseEntity.ok(accounts);
    }
}
