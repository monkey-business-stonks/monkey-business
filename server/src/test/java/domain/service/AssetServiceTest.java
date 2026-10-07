package domain.service;

import domain.entities.Asset;
import domain.entities.Account;
import domain.repository.AssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private AssetService assetService;

    private Account testAccount;
    private Asset testAsset;

    @BeforeEach
    void setUp() {
        testAccount = new Account(
            UUID.randomUUID(),
            ZonedDateTime.now(),
            Account.AccountType.BROKERAGE,
            BigDecimal.valueOf(50000),
            BigDecimal.valueOf(50000),
            new java.util.HashSet<>(),
            new java.util.HashSet<>()
        );

        testAsset = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            "Apple Inc.",
            100.0,
            BigDecimal.valueOf(150.00)
        );
        testAsset.setAccount(testAccount);
    }

    @Test
    void testUpdateAssetOnBuy_NewAsset() {
        // Arrange
        when(assetRepository.findByAccountAccountIdAndTicker(testAccount.getAccID(), "AAPL"))
            .thenReturn(Optional.empty());
        when(assetRepository.save(any(Asset.class))).thenReturn(testAsset);

        // Act
        Asset result = assetService.updateAssetOnBuy(testAccount, "aapl", 100.0, BigDecimal.valueOf(150.00));

        // Assert
        assertNotNull(result);
        assertEquals("AAPL", result.ticker());
        assertEquals(100.0, result.quantity());
        verify(assetRepository, times(1)).save(any(Asset.class));
    }

    @Test
    void testUpdateAssetOnBuy_ExistingAsset_UpdatesAverageCost() {
        // Arrange
        Asset existingAsset = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            "Apple Inc.",
            100.0,
            BigDecimal.valueOf(150.00)
        );
        existingAsset.setAccount(testAccount);

        when(assetRepository.findByAccountAccountIdAndTicker(testAccount.getAccID(), "AAPL"))
            .thenReturn(Optional.of(existingAsset));
        when(assetRepository.save(existingAsset)).thenReturn(existingAsset);

        // Act
        Asset result = assetService.updateAssetOnBuy(testAccount, "aapl", 100.0, BigDecimal.valueOf(160.00));

        // Assert
        assertNotNull(result);
        assertEquals(200.0, result.quantity()); // 100 + 100
        // Average: (100*150 + 100*160) / 200 = 155
        assertTrue(result.averageCost().compareTo(BigDecimal.valueOf(155.00)) == 0);
    }

    @Test
    void testUpdateAssetOnSell_AssetExists_ReducesQuantity() {
        // Arrange
        Asset existingAsset = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            "Apple Inc.",
            100.0,
            BigDecimal.valueOf(150.00)
        );
        existingAsset.setAccount(testAccount);

        when(assetRepository.findByAccountAccountIdAndTicker(testAccount.getAccID(), "AAPL"))
            .thenReturn(Optional.of(existingAsset));
        when(assetRepository.save(existingAsset)).thenReturn(existingAsset);

        // Act
        assetService.updateAssetOnSell(testAccount, "aapl", 30.0);

        // Assert
        assertEquals(70.0, existingAsset.quantity()); // 100 - 30
        verify(assetRepository, times(1)).save(existingAsset);
        verify(assetRepository, never()).delete(existingAsset);
    }

    @Test
    void testUpdateAssetOnSell_AssetExists_DeletesWhenQuantityReaches0() {
        // Arrange
        Asset existingAsset = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            "Apple Inc.",
            100.0,
            BigDecimal.valueOf(150.00)
        );
        existingAsset.setAccount(testAccount);

        when(assetRepository.findByAccountAccountIdAndTicker(testAccount.getAccID(), "AAPL"))
            .thenReturn(Optional.of(existingAsset));

        // Act
        assetService.updateAssetOnSell(testAccount, "aapl", 100.0);

        // Assert
        verify(assetRepository, times(1)).delete(existingAsset);
        verify(assetRepository, never()).save(any());
    }

    @Test
    void testUpdateAssetOnSell_AssetNotFound() {
        // Arrange
        when(assetRepository.findByAccountAccountIdAndTicker(testAccount.getAccID(), "UNKNOWN"))
            .thenReturn(Optional.empty());

        // Act
        assetService.updateAssetOnSell(testAccount, "unknown", 50.0);

        // Assert
        verify(assetRepository, never()).save(any());
        verify(assetRepository, never()).delete(any());
    }

    @Test
    void testGetAsset_Found() {
        // Arrange
        when(assetRepository.findByAccountAccountIdAndTicker(testAccount.getAccID(), "AAPL"))
            .thenReturn(Optional.of(testAsset));

        // Act
        Asset result = assetService.getAsset(testAccount, "aapl");

        // Assert
        assertNotNull(result);
        assertEquals("AAPL", result.ticker());
    }

    @Test
    void testGetAsset_NotFound() {
        // Arrange
        when(assetRepository.findByAccountAccountIdAndTicker(testAccount.getAccID(), "UNKNOWN"))
            .thenReturn(Optional.empty());

        // Act
        Asset result = assetService.getAsset(testAccount, "UNKNOWN");

        // Assert
        assertNull(result);
    }

    @Test
    void testUpdateAssetOnBuy_CaseSensitive() {
        // Arrange
        when(assetRepository.findByAccountAccountIdAndTicker(testAccount.getAccID(), "TSLA"))
            .thenReturn(Optional.empty());
        when(assetRepository.save(any(Asset.class))).thenReturn(testAsset);

        // Act
        assetService.updateAssetOnBuy(testAccount, "TsLa", 50.0, BigDecimal.valueOf(200.00));

        // Assert
        ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(captor.capture());
        assertEquals("TSLA", captor.getValue().ticker());
    }

    @Test
    void testUpdateAssetOnSell_CaseSensitive() {
        // Arrange
        Asset existingAsset = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "GOOG",
            "Alphabet Inc.",
            50.0,
            BigDecimal.valueOf(2800.00)
        );
        existingAsset.setAccount(testAccount);

        when(assetRepository.findByAccountAccountIdAndTicker(testAccount.getAccID(), "GOOG"))
            .thenReturn(Optional.of(existingAsset));
        when(assetRepository.save(existingAsset)).thenReturn(existingAsset);

        // Act
        assetService.updateAssetOnSell(testAccount, "GoOg", 20.0);

        // Assert
        assertEquals(30.0, existingAsset.quantity());
        verify(assetRepository).save(existingAsset);
    }
}
