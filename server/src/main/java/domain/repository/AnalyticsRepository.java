package domain.repository;

import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AnalyticsRepository {

    private final NamedParameterJdbcTemplate analyticsJdbcTemplate;

    public AnalyticsRepository(@Qualifier("analyticsJdbcTemplate") NamedParameterJdbcTemplate analyticsJdbcTemplate) {
        this.analyticsJdbcTemplate = analyticsJdbcTemplate;
    }

    public Map<String, Object> fetchReplicaHealth() {
        String sql = """
            SELECT
                pg_is_in_recovery() AS is_replica,
                current_setting('transaction_read_only') = 'on' AS is_read_only,
                COALESCE(EXTRACT(EPOCH FROM now() - pg_last_xact_replay_timestamp())::bigint, 0) AS replay_lag_seconds,
                pg_last_wal_receive_lsn() IS NOT NULL AS receiving_wal
            """;

        return analyticsJdbcTemplate.queryForMap(sql, Map.of());
    }

    public Map<String, Object> fetchPlatformOverviewCounts() {
        String sql = """
            SELECT
                (SELECT COUNT(*) FROM users) AS users_count,
                (SELECT COUNT(*) FROM accounts) AS accounts_count,
                (SELECT COUNT(*) FROM orders) AS orders_count,
                (SELECT COUNT(*) FROM assets) AS assets_count
            """;

        return analyticsJdbcTemplate.queryForMap(sql, Map.of());
    }
}
