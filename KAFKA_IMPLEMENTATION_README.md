# Kafka Implementation Guide - Monkey Business Trading Platform

## Executive Summary

This document outlines the implementation of Apache Kafka to solve the critical performance bottleneck in the Monkey Business trading platform: **order processing currently takes 30-100+ seconds due to synchronous blocking operations.**

**Solution**: Implement Kafka for async order processing + Caffeine for price caching
- **Target latency**: 1-2 seconds per order (50-100x improvement)
- **Cost savings**: 90% reduction in API calls via Caffeine caching
- **Implementation time**: 2-3 weeks in 2 phases

---

## Problem Statement

### Current Architecture (Problematic)
```
User submits order
    ↓ (blocking)
Backend validates order
    ↓ (calls external API)
Backend executes trade
    ↓ (waits 10+ seconds for API)
Backend updates portfolio
    ↓ (recalculates for every asset)
Backend calls API for each asset price
    ↓ (50+ sequential API calls)
User gets response after 30-100+ seconds ❌
```

**Issues**:
- Order processing takes 30-100+ seconds
- Each order makes 50+ API calls to Fauxnance
- User sees spinner for entire duration
- System can't handle >1 order/second
- No way to notify user when order completes

---

## Solution: Kafka + Caffeine

### New Architecture (Optimized)
```
User submits order
    ↓
Backend quick validation + publish to Kafka
    ↓
Return HTTP 202 immediately (< 100ms) ✅
    ↓
[Background] Kafka consumers process async
    ├─ Validate order (use Caffeine cache for prices)
    ├─ Execute trade
    ├─ Update portfolio
    └─ Persist to database
    ↓
Consumers complete within 1-2 seconds
    ↓
Next scheduled background task
    ↓
Order marked as FILLED in database
```

**Benefits**:
- ✅ User sees immediate response (< 100ms)
- ✅ Backend processes orders asynchronously
- ✅ Prices cached (90% fewer API calls)
- ✅ Can handle 100+ concurrent orders
- ✅ Scalable to 1000+ orders/day

---

## Architecture Overview

### Components

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (Angular)                        │
│                   POST /orders (async)                       │
└────────────────────────┬────────────────────────────────────┘
                         │
         ┌───────────────┴───────────────┐
         ↓                               ↓
    ┌─────────────┐          [Immediate]┌──────────────────┐
    │   Kafka     │                      │ Spring Boot API  │
    │   Broker    │                      ├──────────────────┤
    │             │                      │ Quick validate   │
    │ Topics:     │                      │ Publish to Kafka │
    │ ├─ orders.* │◄──────┬─────────────┤ Return 202       │
    │ ├─ prices.* │       │             └──────────────────┘
    │ └─ audit.*  │       │
    └──────┬──────┘    [Async Background]
           │
    ┌──────┴────────────────────────────────┐
    │                                       │
    ↓                                       ↓
┌──────────────────┐        ┌──────────────────────┐
│   Consumers      │        │  Caffeine Cache      │
├──────────────────┤        ├──────────────────────┤
│ OrderValidator   │──────→ │ Prices (TTL: 5min)   │
│ OrderExecutor    │        │ Updated every 5min   │
│ OrderPersistence │        └──────────────────────┘
│ AuditLogger      │
│ DLQHandler       │
└──────────────────┘
         │
         ↓
    ┌─────────────┐
    │ PostgreSQL  │
    ├─────────────┤
    │ Orders      │
    │ Accounts    │
    │ Assets      │
    │ Audit Log   │
    └─────────────┘
```

---

## Phased Implementation

### Phase 1: Core Kafka Infrastructure (Week 1)

#### 1.1 Docker Setup
- Add Kafka broker cluster (3 instances for HA)
- Add Zookeeper cluster (3 instances for coordination)
- Update docker-compose.yml with health checks
- Create initialization script for topics

#### 1.2 Maven Dependencies
```xml
<!-- Spring Cloud Stream (Kafka) -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-stream-kafka</artifactId>
</dependency>

<!-- Kafka Client -->
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
</dependency>

<!-- Spring Kafka (additional features) -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>

<!-- Testing -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
```

#### 1.3 Kafka Topics
Create these topics:
- `orders.submitted` (10 partitions, 3x replication)
- `orders.validated` (10 partitions, 3x replication)
- `orders.filled` (10 partitions, 3x replication)
- `orders.rejected` (10 partitions, 3x replication)
- `orders.dlq` (3 partitions, 3x replication) - Dead Letter Queue
- `prices.market-update` (5 partitions, 3x replication)
- `audit.logs` (5 partitions, 3x replication)

#### 1.4 Application Configuration
Add to `application.yml`:
```yaml
spring:
  cloud:
    stream:
      kafka:
        binder:
          brokers: kafka-1:9092,kafka-2:9092,kafka-3:9092
          replication-factor: 3
          auto-create-topics.enabled: false
        bindings:
          publishOrderSubmitted-out-0:
            producer:
              partition-key-expression: headers['account-id']
          orderValidatorConsumer-in-0:
            consumer:
              max-attempts: 3
              back-off-initial-interval: 1000
              back-off-max-interval: 8000
      bindings:
        publishOrderSubmitted-out-0:
          destination: orders.submitted
          contentType: application/json
        orderValidatorConsumer-in-0:
          destination: orders.submitted
          group: order-validator-group
        # ... more bindings
```

---

### Phase 2: Order Processing Pipeline (Week 1-2)

#### 2.1 Event Classes
Create immutable event POJOs:
- `OrderSubmittedEvent` - User places order
- `OrderValidatedEvent` - Validation complete
- `OrderFilledEvent` - Trade executed
- `OrderRejectedEvent` - Validation/execution failed

Each event contains:
- `orderId: UUID`
- `accountId: UUID`
- `userId: UUID`
- `ticker: String`
- `quantity: BigDecimal`
- `action: OrderAction` (BUY/SELL)
- `orderType: OrderType` (EQUITY/CRYPTO/FOREX)
- Metadata (correlation-id, timestamp, source)

#### 2.2 OrderEventPublisher
New service that:
- Accepts order events
- Adds correlation ID for tracing
- Sets Kafka headers (user-id, timestamp, source)
- Publishes to appropriate topic
- Handles errors (retry 3x, then DLQ)

#### 2.3 Modify OrderController
**Before**:
```java
@PostMapping("/orders")
public ResponseEntity<?> placeOrder(...) {
    // Synchronous: validate + execute + return
    Order result = orderService.placeOrder(...);
    return ResponseEntity.ok(result);
}
```

**After**:
```java
@PostMapping("/orders")
public ResponseEntity<?> placeOrder(...) {
    // Quick validation only
    validateInput(request);
    
    // Create event
    OrderSubmittedEvent event = new OrderSubmittedEvent(...)
    
    // Publish to Kafka
    orderEventPublisher.publishOrderSubmitted(event);
    
    // Return immediately
    return ResponseEntity.accepted().body({
        orderId: event.getOrderId(),
        status: "PENDING",
        submittedAt: LocalDateTime.now()
    });
}
```

#### 2.4 OrderValidator Consumer
New consumer that:
- Listens to `orders.submitted` topic
- Validates order business rules:
  - Account exists
  - Account type ↔ asset type compatibility
  - Cash available for BUY orders
  - Quantity > 0
  - Action is BUY/SELL
- Uses Caffeine cache for prices (Phase 2)
- Publishes to `orders.validated` topic
- Handles errors (retry, then DLQ)

#### 2.5 OrderExecutor Consumer
New consumer that:
- Listens to `orders.validated` topic
- Checks if validation passed
- Fetches FRESH price from API (not cache)
- Calculates execution value
- Updates Order entity (status = FILLED)
- Publishes to `orders.filled` topic
- Handles errors (calls PricingEngine failure logic)

#### 2.6 OrderPersistence Consumer
New consumer that:
- Listens to `orders.filled` and `orders.rejected`
- Updates Order table with final status
- Calls OrderExecutionEngine.updateAccount()
  - Adjusts cash balance
  - Updates holdings (AssetService)
  - Recalculates portfolio balance
- Saves to database
- Publishes AuditEvent

#### 2.7 Database Schema Changes
Add to Order table:
```sql
ALTER TABLE orders 
ADD COLUMN kafka_message_id VARCHAR(200),
ADD COLUMN correlation_id UUID,
ADD UNIQUE INDEX idx_kafka_message_id ON orders(kafka_message_id);
```

Create new tables:
```sql
CREATE TABLE order_events (
    event_id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    event_type VARCHAR(50),
    event_payload JSONB,
    kafka_message_id VARCHAR(200) UNIQUE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE audit_log (
    audit_id UUID PRIMARY KEY,
    user_id UUID,
    action VARCHAR(50),
    resource_id UUID,
    details JSONB,
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT NOW()
);
```

#### 2.8 Error Handling & DLQ
- All consumers retry failed messages 3 times
- Exponential backoff: 1s, 2s, 4s
- After 3 failures → send to `orders.dlq`
- DLQ consumer logs error and alerts ops
- Admin endpoint to retry DLQ messages

---

### Phase 3: Price Caching with Caffeine (Week 2)

#### 3.1 Add Caffeine Dependency
```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

#### 3.2 Configure Caffeine
Add to `application.yml`:
```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: expireAfterWrite=5m,maximumSize=10000

logging:
  level:
    org.springframework.cache: INFO
```

#### 3.3 Create PriceCacheService
```java
@Service
public class PriceCacheService {
    
    @Cacheable(value = "prices", key = "#ticker")
    public Optional<BigDecimal> getCachedPrice(String ticker) {
        // Cached automatically for 5 minutes
        // OrderValidators call this instead of API
        return // get from cache or return empty
    }
    
    @CacheEvict(value = "prices", allEntries = true)
    public void invalidateAll() {
        // Manual refresh (called by scheduler)
    }
}
```

#### 3.4 Create PriceUpdateScheduler
```java
@Service
public class PriceUpdateScheduler {
    
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void updatePrices() {
        // Get active tickers from database
        List<String> tickers = getActiveTickersFromDatabase();
        
        // Batch fetch prices (max 25 per API call)
        Map<String, BigDecimal> prices = pricingEngine.getQuotes(tickers);
        
        // Publish to Kafka (optional, for audit)
        kafkaTemplate.send("prices.market-update", prices);
        
        // Cache is automatically updated via @Cacheable
        prices.forEach((ticker, price) -> {
            priceCacheService.getCachedPrice(ticker);
        });
    }
}
```

#### 3.5 Update OrderValidator to Use Cache
```java
// Before: called API directly
BigDecimal price = pricingEngine.getPrice(ticker); // API call

// After: use cache
Optional<BigDecimal> cachedPrice = priceCacheService.getCachedPrice(ticker);
BigDecimal price = cachedPrice.orElseGet(() -> 
    pricingEngine.getPrice(ticker) // Fallback if cache empty
);
```

---

### Phase 4: Testing & Monitoring (Week 2-3)

#### 4.1 Unit Tests
- Test each consumer independently
- Test idempotency (duplicate messages)
- Test error handling

#### 4.2 Integration Tests
- Use embedded Kafka (spring-kafka-test)
- End-to-end: order submitted → filled
- Test failure scenarios

#### 4.3 Load Tests
- 100 concurrent orders
- Verify all complete in < 5 seconds
- Monitor consumer lag

#### 4.4 Monitoring
- Add Prometheus metrics:
  - `kafka.consumer.lag` - How far behind?
  - `order.processing.latency` - Time to complete
  - `order.submitted.total` - Total orders
- Create Grafana dashboards
- Setup health checks

---

## Key Design Decisions

### 1. Why Kafka?
- ✅ Decouples order submission from processing
- ✅ Enables async/background processing
- ✅ Natural fit for event-driven architecture
- ✅ Scalable to 1000+ orders/day
- ✅ Built-in fault tolerance (replication)

### 2. Why Caffeine (Not Redis)?
- ✅ In-memory caching (microsecond lookups)
- ✅ No external service needed (fewer moving parts)
- ✅ Prices expire every 5 minutes anyway (loss is acceptable)
- ✅ Easy to migrate to Redis later (same `@Cacheable` annotation)
- ✅ Zero setup time (just 1 dependency)

### 3. Why 5-Minute Price Updates?
- Fauxnance API only updates prices every 5 minutes
- No need for real-time streaming
- Batch caching eliminates 90% of API calls
- Matches natural API refresh cycle

### 4. Event Sourcing Approach
- Every state change is an event (audit trail)
- Events published to Kafka topics
- Multiple consumers can react (flexibility)
- Order history completely reconstructable

### 5. Idempotency
- Use `kafka_message_id` to detect duplicates
- Idempotent operations (safe to retry)
- Prevents double-charging customers

---

## Implementation Checklist

### Infrastructure
- [ ] Update docker-compose.yml with Kafka cluster
- [ ] Update docker-compose.yml with Zookeeper cluster
- [ ] Create script to initialize Kafka topics
- [ ] Verify all services start and health checks pass

### Maven
- [ ] Add spring-cloud-starter-stream-kafka
- [ ] Add spring-kafka
- [ ] Add spring-kafka-test
- [ ] Add caffeine dependency

### Configuration
- [ ] Update application.yml with Kafka broker addresses
- [ ] Configure Caffeine cache (expireAfterWrite=5m)
- [ ] Setup logging configuration
- [ ] Create application-dev.yml and application-prod.yml

### Event Classes
- [ ] Create OrderEvent base class
- [ ] Create OrderSubmittedEvent
- [ ] Create OrderValidatedEvent
- [ ] Create OrderFilledEvent
- [ ] Create OrderRejectedEvent
- [ ] Create PriceUpdateEvent

### Producer
- [ ] Create OrderEventPublisher service
- [ ] Modify OrderController
- [ ] Add quick validation logic
- [ ] Return HTTP 202 (Accepted)

### Consumers
- [ ] Create OrderValidatorConsumer
- [ ] Create OrderExecutorConsumer
- [ ] Create OrderPersistenceConsumer
- [ ] Create AuditLoggerConsumer
- [ ] Create DLQConsumer
- [ ] Test each consumer independently

### Caching
- [ ] Create PriceCacheService
- [ ] Create PriceUpdateScheduler
- [ ] Update OrderValidator to use cache
- [ ] Verify 90% API reduction

### Database
- [ ] Add kafka_message_id to Order table
- [ ] Add correlation_id to Order table
- [ ] Create order_events table
- [ ] Create audit_log table
- [ ] Verify schema changes

### Testing
- [ ] Unit tests for all consumers
- [ ] Integration tests (embedded Kafka)
- [ ] Load test (100 concurrent orders)
- [ ] Idempotency tests
- [ ] Error scenario tests

### Monitoring
- [ ] Add Prometheus metrics
- [ ] Create Grafana dashboard
- [ ] Setup health check endpoint
- [ ] Configure alerting

### Documentation
- [ ] Document Kafka topics
- [ ] Document consumer groups
- [ ] Document event schemas
- [ ] Create troubleshooting guide

---

## Expected Performance Improvements

### Latency
| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| Order response time | 30-100s | < 100ms | 300-1000x |
| Order completion time | 30-100s | 1-2s | 15-100x |
| Price lookup | API call (5-10s) | Cache (μs) | 10,000x |

### Cost
| Metric | Before | After | Savings |
|--------|--------|-------|---------|
| API calls/order | 50+ | 5 (batch) | 90% |
| Daily API calls | 50,000 | 288 | 99.4% |
| Monthly API cost | $500-5000 | $3-30 | 98% |

### Throughput
| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| Orders/second | <1 | 100+ | 100x+ |
| Concurrent users | ~100 | 1000+ | 10x |
| Daily capacity | 50-100 | 100,000+ | 1000x |

---

## Timeline

- **Week 1**: 
  - Days 1-2: Docker + Kafka infrastructure
  - Days 3-4: Maven + Configuration
  - Day 5: Event classes + OrderEventPublisher

- **Week 2**:
  - Days 1-2: Consumers (Validator, Executor, Persistence)
  - Day 3: Database schema changes
  - Days 4-5: Caffeine caching + PriceUpdateScheduler

- **Week 3**:
  - Days 1-2: Testing (unit + integration + load)
  - Days 3-4: Error handling + DLQ
  - Day 5: Monitoring + Grafana dashboards

**Total**: 2-3 weeks (50-75 hours of work)

---

## Risk Mitigation

### Risk: Message Loss
- **Mitigation**: Kafka replication factor 3, acks=all

### Risk: Consumer Lag (too many events)
- **Mitigation**: Monitor lag, auto-scale consumers, alert if lag > 1000

### Risk: Duplicate Processing
- **Mitigation**: Idempotency check on kafka_message_id

### Risk: Kafka Broker Down
- **Mitigation**: 3-broker cluster (fault tolerant)

### Risk: Database Connection Pool Exhausted
- **Mitigation**: Limit consumer concurrency, monitor connection pool

---

## Rollback Plan

If Kafka integration has issues:

1. **Stop all Kafka consumers** (keep publisher)
2. **Modify OrderController** to fall back to synchronous mode
3. **Restart Spring Boot** with old OrderService logic
4. System reverts to original behavior (slow but functional)

Kafka is additive, not required. Can be disabled by comments.

---

## Next Steps

1. Review this plan with team
2. Set up development environment with docker-compose
3. Begin Phase 1: Infrastructure setup
4. Daily standup on progress
5. Integration tests after each phase

---

## Questions & Clarifications

**Q: Do we need to change the frontend?**  
A: Minimal changes. Orders return HTTP 202 instead of 201. Frontend handles PENDING status.

**Q: Can we roll back if needed?**  
A: Yes. Kafka is optional, can disable consumers and fall back to sync mode.

**Q: Will this affect existing order history?**  
A: No. Database schema unchanged (just new columns added). Old orders unaffected.

**Q: When can we go to production?**  
A: After Phase 2 (week 2) testing. Start with 10% of traffic, gradually increase.

**Q: Do we need ops team for Kafka monitoring?**  
A: Minimal. Health checks + alerts handle most issues. Can contact vendor support if needed.

---

**Document Version**: 1.0  
**Last Updated**: 2026-10-07  
**Status**: Ready for implementation
