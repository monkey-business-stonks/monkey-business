package domain.controller;


import domain.dto.*;
import domain.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/users/{userId}/accounts")
@CrossOrigin(origins = "*")
public class AccountController {

    @Autowired
    private AccountService accountService;

    /**
     * Create account for user
     */
    @PostMapping
    public ResponseEntity<?> createAccount(
            @PathVariable String userId,
            @RequestBody CreateAccountRequest request) {
        try {
            UUID id = UUID.fromString(userId);
            AccountResponse account = accountService.createAccount(id, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(account);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse()
                    .message(e.getMessage())
                    .error("INVALID_INPUT"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse()
                    .message(e.getMessage())
                    .error("NOT_FOUND"));
        } catch (Exception e) {
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
            List<AccountResponse> accounts = accountService.listAccounts(id);
            return ResponseEntity.ok(accounts);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse()
                    .message("Invalid user ID format")
                    .error("INVALID_ID"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse()
                    .message(e.getMessage())
                    .error("NOT_FOUND"));
        }
    }

    /**
     * Get specific account for a user
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<?> getAccount(
            @PathVariable String userId,
            @PathVariable String accountId) {
        try {
            UUID userIdUuid = UUID.fromString(userId);
            UUID accountIdUuid = UUID.fromString(accountId);
            AccountResponse account = accountService.getAccount(accountIdUuid);
            // Verify the account belongs to the user
            if (!account.getUserId().equals(userIdUuid)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse()
                        .message("Account does not belong to this user")
                        .error("FORBIDDEN"));
            }
            return ResponseEntity.ok(account);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse()
                    .message("Invalid ID format")
                    .error("INVALID_ID"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse()
                    .message(e.getMessage())
                    .error("NOT_FOUND"));
        }
    }
}
