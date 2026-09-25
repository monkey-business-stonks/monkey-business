package main.controller;

import main.dto.*;
import main.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/accounts/{accountId}/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * Place a new order
     * POST /api/accounts/{accountId}/orders
     */
    @PostMapping
    public ResponseEntity<?> placeOrder(
            @PathVariable String accountId,
            @RequestBody PlaceOrderRequest request) {
        try {
            UUID id = UUID.fromString(accountId);
            OrderResponse order = orderService.placeOrder(id, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage(), "INVALID_ORDER"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage(), "NOT_FOUND"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to place order: " + e.getMessage(), "SERVER_ERROR"));
        }
    }

    /**
     * Get order history for an account
     * GET /api/accounts/{accountId}/orders
     */
    @GetMapping
    public ResponseEntity<?> listOrders(
            @PathVariable String accountId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String ticker) {
        try {
            UUID id = UUID.fromString(accountId);
            List<OrderSummary> orders = orderService.listOrdersForAccount(id);
            
            // Filter by status if provided
            if (status != null && !status.isEmpty()) {
                orders = orders.stream()
                    .filter(o -> o.getStatus().equalsIgnoreCase(status))
                    .toList();
            }
            
            // Filter by ticker if provided
            if (ticker != null && !ticker.isEmpty()) {
                orders = orders.stream()
                    .filter(o -> o.getTicker().equalsIgnoreCase(ticker))
                    .toList();
            }
            
            return ResponseEntity.ok(orders);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Invalid account ID format", "INVALID_ID"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage(), "NOT_FOUND"));
        }
    }
}
