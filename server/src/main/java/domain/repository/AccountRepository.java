package domain.repository;

import domain.entities.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    // List all accounts for user
    List<Account> findByUserUserId(UUID userId);
    
    // Get specific account
    Optional<Account> findById(UUID accountId);
    
    // Get accounts by type
    @Query(value = "SELECT a.* FROM Accounts a WHERE a.userId = :userId AND a.accountType = :type", 
           nativeQuery = true)
    List<Account> findByUserIdAndAccountType(@Param("userId") UUID userId, 
                                             @Param("type") String accountType);
    
    // ===== ATOMIC OPERATIONS (for transaction safety) =====
    
    // Deduct balance (fails if insufficient funds - prevents overdraft)
    @Modifying
    @Query(value = "UPDATE Accounts SET balance = balance - :amount, updatedAt = CURRENT_TIMESTAMP " +
           "WHERE accountId = :id AND balance >= :amount", 
           nativeQuery = true)
    int deductBalance(@Param("id") UUID accountId, @Param("amount") BigDecimal amount);
    
    // Credit balance (add funds back after SELL)
    @Modifying
    @Query(value = "UPDATE Accounts SET balance = balance + :amount, updatedAt = CURRENT_TIMESTAMP " +
           "WHERE accountId = :id", 
           nativeQuery = true)
    int creditBalance(@Param("id") UUID accountId, @Param("amount") BigDecimal amount);
    
    // Update cash balance
    @Modifying
    @Query(value = "UPDATE Accounts SET cashBalance = cashBalance + :amount, updatedAt = CURRENT_TIMESTAMP " +
           "WHERE accountId = :id", 
           nativeQuery = true)
    int updateCashBalance(@Param("id") UUID accountId, @Param("amount") BigDecimal amount);
}
