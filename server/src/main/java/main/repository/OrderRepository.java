package main.repository;

import main.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByAccountAccountID(UUID accountID);
    List<Order> findByAccountAccountIDAndStatus(UUID accountID, Order.OrderStatus status);
}
