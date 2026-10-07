package domain.controller;

import domain.dto.*;
import domain.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/accounts/{accountId}/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    /**
     * Place a new order
     * POST /api/accounts/{accountId}/orders
     */
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @PathVariable String accountId,
            @Valid @RequestBody PlaceOrderRequest request) {
        UUID id = UUID.fromString(accountId);
        logger.info("Placing order for account: {}, ticker: {}", accountId, request.getTicker());
        OrderResponse order = orderService.placeOrder(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * Get order history for an account
     * GET /api/accounts/{accountId}/orders
     */
    @GetMapping
    public ResponseEntity<List<OrderSummary>> listOrders(
            @PathVariable String accountId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String ticker) {
        UUID id = UUID.fromString(accountId);
        logger.debug("Listing orders for account: {}, filters - status: {}, ticker: {}", accountId, status, ticker);
        List<OrderSummary> orders = orderService.listOrdersForAccount(id);
        
        // Filter by status if provided
        if (status != null && !status.isEmpty()) {
            orders = orders.stream()
                .filter(o -> o.getStatus().toString().equalsIgnoreCase(status))
                .toList();
        }
        
        // Filter by ticker if provided
        if (ticker != null && !ticker.isEmpty()) {
            orders = orders.stream()
                .filter(o -> o.getTicker().equalsIgnoreCase(ticker))
                .toList();
        }
        
        return ResponseEntity.ok(orders);
    }
}
