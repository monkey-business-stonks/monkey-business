package domain.controller;

import domain.dto.AccountResponse;
import domain.dto.ErrorResponse;
import domain.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/users/{userId}/accounts")
@CrossOrigin(origins = "*")
public class AccountDetailController {

    private static final Logger logger = LoggerFactory.getLogger(AccountDetailController.class);

    @Autowired
    private AccountService accountService;

    /**
     * Get account by ID
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<?> getAccount(
            @PathVariable String userId,
            @PathVariable String accountId) {
        try {
            // TODO: Use userId_uuid to verify account ownership once auth is integrated
            // UUID userId_uuid = UUID.fromString(userId);
            UUID id = UUID.fromString(accountId);
            AccountResponse account = accountService.getAccount(id);
            if (account == null) {
                logger.warn("Account not found for ID: {}", accountId);
                return ResponseEntity.status(404)
                    .body(new ErrorResponse()
                        .message("Account not found")
                        .error("NOT_FOUND"));
            }
            return ResponseEntity.ok(account);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid ID format: userId: {}, accountId: {}", userId, accountId);
            return ResponseEntity.badRequest()
                .body(new ErrorResponse()
                    .message("Invalid user ID or account ID format")
                    .error("INVALID_ID"));
        } catch (NoSuchElementException e) {
            logger.warn("Account not found for ID: {}", accountId);
            return ResponseEntity.status(404)
                .body(new ErrorResponse()
                    .message(e.getMessage())
                    .error("NOT_FOUND"));
        } catch (Exception e) {
            logger.error("Error retrieving account: {}", accountId, e);
            return ResponseEntity.status(500)
                .body(new ErrorResponse()
                    .message("Failed to retrieve account")
                    .error("SERVER_ERROR"));
        }
    }
}
