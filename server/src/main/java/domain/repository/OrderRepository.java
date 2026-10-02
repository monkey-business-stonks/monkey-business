package domain.repository;

import domain.entities.Order;
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
public interface OrderRepository extends JpaRepository<Order, UUID> {
    // Get all orders for account
    List<Order> findByAccountAccountID(UUID accountID);
    
    // Get orders by status
    List<Order> findByAccountAccountIDAndStatus(UUID accountID, Order.OrderStatus status);
    
    // Get specific order
    Optional<Order> findById(UUID orderId);
    
    // Find pending (SUBMITTED) orders for account
    @Query(value = "SELECT o.* FROM Orders o WHERE o.accountId = :accountId " +
           "AND o.status = 'SUBMITTED'", 
           nativeQuery = true)
    List<Order> findPendingOrdersByAccount(@Param("accountId") UUID accountId);
    
    // Find orders by account and ticker
    @Query(value = "SELECT o.* FROM Orders o WHERE o.accountId = :accountId " +
           "AND o.ticker = :ticker ORDER BY o.submittedOn DESC", 
           nativeQuery = true)
    List<Order> findOrdersByAccountAndTicker(@Param("accountId") UUID accountId, 
                                             @Param("ticker") String ticker);
    
    // Get recent orders for account (with limit)
    @Query(value = "SELECT o.* FROM Orders o WHERE o.accountId = :accountId " +
           "ORDER BY o.submittedOn DESC LIMIT :limit", 
           nativeQuery = true)
    List<Order> findRecentOrdersByAccount(@Param("accountId") UUID accountId, 
                                          @Param("limit") int limit);
    
    // Count filled orders for account
    @Query(value = "SELECT COUNT(*) FROM Orders WHERE accountId = :accountId " +
           "AND status = 'FILLED'", 
           nativeQuery = true)
    long countFilledOrdersByAccount(@Param("accountId") UUID accountId);
    
    // ===== ATOMIC OPERATIONS (for transaction safety) =====
    
    // Set order to FILLED with execution details
    @Modifying
    @Query(value = "UPDATE Orders SET status = 'FILLED', statusCode = :statusCode, " +
           "executedOn = CURRENT_TIMESTAMP, executedValue = :executedValue " +
           "WHERE orderId = :id AND status != 'FILLED'", 
           nativeQuery = true)
    int fillOrder(@Param("id") UUID orderId, 
                  @Param("statusCode") Integer statusCode, 
                  @Param("executedValue") BigDecimal executedValue);
    
    // Set order to ACCEPTED status
    @Modifying
    @Query(value = "UPDATE Orders SET status = 'ACCEPTED', statusCode = :statusCode, " +
           "updatedAt = CURRENT_TIMESTAMP WHERE orderId = :id", 
           nativeQuery = true)
    int acceptOrder(@Param("id") UUID orderId, @Param("statusCode") Integer statusCode);
    
    // Set order to REJECTED status
    @Modifying
    @Query(value = "UPDATE Orders SET status = 'REJECTED', statusCode = :statusCode " +
           "WHERE orderId = :id", 
           nativeQuery = true)
    int rejectOrder(@Param("id") UUID orderId, @Param("statusCode") Integer statusCode);
}
