package domain.controller;

import domain.dto.OrderResponse;
import domain.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users/{userId}/orders")
@CrossOrigin(origins = "*")
public class UserOrderController {

    private static final Logger logger = LoggerFactory.getLogger(UserOrderController.class);

    @Autowired
    private OrderService orderService;

    @GetMapping
    @PreAuthorize("@securityService.isUserOwner(authentication, T(java.util.UUID).fromString(#userId)) or hasAuthority('ROLE_OPERATIONS')")
    public ResponseEntity<List<OrderResponse>> listOrders(@PathVariable String userId) {
        UUID id = UUID.fromString(userId);
        logger.debug("Listing orders for user: {}", userId);
        return ResponseEntity.ok(orderService.listOrdersForUser(id));
    }
}