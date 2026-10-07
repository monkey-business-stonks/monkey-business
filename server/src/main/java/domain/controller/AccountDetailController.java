package domain.controller;

import domain.dto.AccountResponse;
import domain.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable String userId,
            @PathVariable String accountId) {
        // TODO: Use userId_uuid to verify account ownership once auth is integrated
        // UUID userId_uuid = UUID.fromString(userId);
        UUID id = UUID.fromString(accountId);
        logger.debug("Retrieving account: {} for user: {}", accountId, userId);
        AccountResponse account = accountService.getAccount(id);
        return ResponseEntity.ok(account);
    }
}
