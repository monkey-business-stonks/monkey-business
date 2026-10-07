package domain.controller;

import domain.dto.OrderResponse;
import domain.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@RestController
@RequestMapping("/accounts/{accountId}/orders")
@CrossOrigin(origins = "*")
public class OrderDetailController {

    private static final Logger logger = LoggerFactory.getLogger(OrderDetailController.class);

    @Autowired
    private OrderService orderService;

    /**
     * Get order details by ID
     * GET /api/accounts/{accountId}/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable String accountId,
            @PathVariable String orderId) {
        // TODO: Use accountId_uuid to verify order ownership once auth is integrated
        // UUID accountId_uuid = UUID.fromString(accountId);
        UUID id = UUID.fromString(orderId);
        logger.debug("Retrieving order: {} for account: {}", orderId, accountId);
        OrderResponse order = orderService.getOrder(id);
        return ResponseEntity.ok(order);
    }
}
