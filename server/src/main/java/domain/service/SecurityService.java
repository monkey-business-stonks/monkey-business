package domain.security;
import domain.repository.AccountRepository;
import domain.repository.OrderRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service("securityService")
public class SecurityService {

    private final AccountRepository accountRepository;
    private final OrderRepository orderRepository;

    public SecurityService(AccountRepository accountRepository, OrderRepository orderRepository) {
        this.accountRepository = accountRepository;
        this.orderRepository = orderRepository;
    }

    public boolean isAccountOwner(Authentication authentication, UUID accountId) {
        if (authentication == null) return false;
        try {
            UUID principalUserId = UUID.fromString(authentication.getName());
            return accountRepository.findById(accountId)
                    .map(account -> account.getUserId().equals(principalUserId))
                    .orElse(false);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean isOrderOwner(Authentication authentication, UUID orderId) {
        if (authentication == null) return false;
        try {
            UUID principalUserId = UUID.fromString(authentication.getName());
            return orderRepository.findById(orderId)
                    .map(order -> order.getAccount().getUserId().equals(principalUserId))
                    .orElse(false);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean isUserOwner(Authentication authentication, UUID userId) {
        if (authentication == null) return false;
        try {
            UUID principalUserId = UUID.fromString(authentication.getName());
            return principalUserId.equals(userId);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}