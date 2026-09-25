package main.controller;

import main.dto.AccountResponse;
import main.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@CrossOrigin(origins = "*")
public class AccountDetailController {

    @Autowired
    private AccountService accountService;

    /**
     * Get account by ID
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<?> getAccount(@PathVariable String accountId) {
        try {
            UUID id = UUID.fromString(accountId);
            AccountResponse account = accountService.getAccount(id);
            return ResponseEntity.ok(account);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
