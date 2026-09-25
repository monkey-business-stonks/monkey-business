package main.repository;

import main.entity.AssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssetRepository extends JpaRepository<AssetEntity, UUID> {
    List<AssetEntity> findByAccountAccountId(UUID accountId);
    Optional<AssetEntity> findByAccountAccountIdAndTicker(UUID accountId, String ticker);
}
