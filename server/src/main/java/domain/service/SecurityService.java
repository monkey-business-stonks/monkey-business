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
        String currentUserId = authentication.getName(); 
        
        return accountRepository.findById(accountId)
                .map(account -> account.getUserId().toString().equals(currentUserId))
                .orElse(false);
    }

    public boolean isOrderOwner(Authentication authentication, UUID orderId) {
        if (authentication == null) return false;
        String currentUserId = authentication.getName();
        
        return orderRepository.findById(orderId)
                .map(order -> order.getAccount().getUserId().toString().equals(currentUserId))
                .orElse(false);
    }
}