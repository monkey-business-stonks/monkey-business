package domain.controller;

import domain.dto.AccountResponse;
import domain.service.AccountService;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @GetMapping("/{accountId}")
    @PreAuthorize("(#userId == authentication.name and @securityService.isAccountOwner(authentication, T(java.util.UUID).fromString(#accountId))) or hasAuthority('ROLE_OPERATIONS')")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable String userId,
            @PathVariable String accountId) {
        UUID id = UUID.fromString(accountId);
        logger.debug("Retrieving account: {} for user: {}", accountId, userId);
        AccountResponse account = accountService.getAccount(id);
        return ResponseEntity.ok(account);
    }
}
