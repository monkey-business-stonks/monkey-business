package domain.controller;


import domain.dto.*;
import domain.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/users/{userId}/accounts")
@CrossOrigin(origins = "*")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    @Autowired
    private AccountService accountService;

    /**
     * Create account for user
     */
    @PostMapping
    public ResponseEntity<?> createAccount(
            @PathVariable String userId,
            @Valid @RequestBody CreateAccountRequest request) {
        try {
            UUID id = UUID.fromString(userId);
            logger.info("Creating account for user: {}", userId);
            AccountResponse account = accountService.createAccount(id, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(account);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid user ID format or input for account creation: {}", userId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse()
                    .message(e.getMessage())
                    .error("INVALID_INPUT"));
        } catch (NoSuchElementException e) {
            logger.warn("User not found for account creation: {}", userId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse()
                    .message(e.getMessage())
                    .error("NOT_FOUND"));
        } catch (Exception e) {
            logger.error("Error creating account for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse()
                    .message("Failed to create account")
                    .error("SERVER_ERROR"));
        }
    }

    /**
     * List all accounts for a user
     */
    @GetMapping
    public ResponseEntity<?> listAccounts(@PathVariable String userId) {
        try {
            UUID id = UUID.fromString(userId);
            logger.debug("Listing accounts for user: {}", userId);
            List<AccountResponse> accounts = accountService.listAccounts(id);
            return ResponseEntity.ok(accounts);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid user ID format: {}", userId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse()
                    .message("Invalid user ID format")
                    .error("INVALID_ID"));
        } catch (NoSuchElementException e) {
            logger.warn("User not found: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse()
                    .message(e.getMessage())
                    .error("NOT_FOUND"));
        }
    }
}
