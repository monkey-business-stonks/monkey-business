package domain.service;

import domain.error.AnalyticsUnavailableException;
import domain.repository.AnalyticsRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    @Value("${analytics.max-replica-lag-seconds:300}")
    private long maxReplicaLagSeconds;

    @Value("${analytics.primary-fallback-enabled:false}")
    private boolean primaryFallbackEnabled;

    public AnalyticsService(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    public Map<String, Object> getReplicaHealth() {
        Map<String, Object> health = analyticsRepository.fetchReplicaHealth();
        health.put("max_allowed_lag_seconds", maxReplicaLagSeconds);
        health.put("primary_fallback_enabled", primaryFallbackEnabled);
        health.put("checked_at", Instant.now().toString());
        return health;
    }

    public Map<String, Object> getOverviewMetrics() {
        Map<String, Object> health = getReplicaHealth();
        assertReplicaHealthy(health);

        Map<String, Object> counts = analyticsRepository.fetchPlatformOverviewCounts();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("source", "postgres_replica");
        response.put("health", health);
        response.put("metrics", counts);
        return response;
    }

    private void assertReplicaHealthy(Map<String, Object> health) {
        boolean isReplica = Boolean.TRUE.equals(health.get("is_replica"));
        boolean isReadOnly = Boolean.TRUE.equals(health.get("is_read_only"));
        long replayLagSeconds = extractLong(health.get("replay_lag_seconds"));

        if (!isReplica || !isReadOnly) {
            throw new AnalyticsUnavailableException("Analytics replica is not in read-only recovery mode");
        }

        if (replayLagSeconds > maxReplicaLagSeconds) {
            throw new AnalyticsUnavailableException(
                    "Analytics replica lag " + replayLagSeconds + "s exceeds max " + maxReplicaLagSeconds + "s");
        }
    }

    private long extractLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new AnalyticsUnavailableException("Unexpected lag value type returned from analytics replica");
    }
}
