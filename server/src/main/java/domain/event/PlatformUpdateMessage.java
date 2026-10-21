package domain.event;

import java.time.ZonedDateTime;

public record PlatformUpdateMessage(
    String eventType,
    ZonedDateTime occurredAt
) {
}
