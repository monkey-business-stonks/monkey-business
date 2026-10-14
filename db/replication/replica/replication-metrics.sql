SELECT
    pg_is_in_recovery() AS is_replica,
    pg_last_wal_receive_lsn() AS last_wal_receive_lsn,
    pg_last_wal_replay_lsn() AS last_wal_replay_lsn,
    pg_last_xact_replay_timestamp() AS last_replay_timestamp,
    COALESCE(EXTRACT(EPOCH FROM now() - pg_last_xact_replay_timestamp())::bigint, 0) AS replay_lag_seconds;
