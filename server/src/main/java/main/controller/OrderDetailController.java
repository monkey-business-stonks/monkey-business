package main.controller;

import main.dto.ErrorResponse;
import main.dto.OrderResponse;
import main.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "*")
public class OrderDetailController {

    @Autowired
    private OrderService orderService;

    /**
     * Get order details by ID
     * GET /api/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable String orderId) {
        try {
            UUID id = UUID.fromString(orderId);
            OrderResponse order = orderService.getOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse()
                    .message("Invalid order ID format")
                    .error("INVALID_ID"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
