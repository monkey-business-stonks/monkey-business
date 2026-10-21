package domain.event;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record OrderUpdateMessage(
    String orderId,
    String accountId,
    String status,
    BigDecimal executedValue,
    ZonedDateTime occurredAt,
    String message
) {
}
