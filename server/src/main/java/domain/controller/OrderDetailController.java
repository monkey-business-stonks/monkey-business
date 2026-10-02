package domain.controller;

import domain.dto.ErrorResponse;
import domain.dto.OrderResponse;
import domain.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "*")
public class OrderDetailController {

    private static final Logger logger = LoggerFactory.getLogger(OrderDetailController.class);

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
            if (order == null) {
                logger.warn("Order not found for ID: {}", orderId);
                return ResponseEntity.status(404)
                    .body(new ErrorResponse()
                        .message("Order not found")
                        .error("NOT_FOUND"));
            }
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid order ID format: {}", orderId);
            return ResponseEntity.badRequest()
                .body(new ErrorResponse()
                    .message("Invalid order ID format")
                    .error("INVALID_ID"));
        } catch (NoSuchElementException e) {
            logger.warn("Order not found for ID: {}", orderId);
            return ResponseEntity.status(404)
                .body(new ErrorResponse()
                    .message(e.getMessage())
                    .error("NOT_FOUND"));
        } catch (Exception e) {
            logger.error("Error retrieving order: {}", orderId, e);
            return ResponseEntity.status(500)
                .body(new ErrorResponse()
                    .message("Failed to retrieve order")
                    .error("SERVER_ERROR"));
        }
    }
}
