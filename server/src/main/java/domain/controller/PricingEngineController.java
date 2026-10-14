package domain.controller;

import domain.service.PricingEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/market")
@CrossOrigin(origins = "*")
public class PricingEngineController {

    @Autowired
    private PricingEngine pricingEngine;

    /**
     * Get current market data for a ticker
     * @param ticker Stock ticker symbol (e.g., AAPL)
     * @return Market data from Fauxnance API
     */
    @GetMapping("/price/{ticker}")
    public ResponseEntity<?> getPrice(@PathVariable String ticker) {
        if (ticker == null || ticker.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ticker cannot be empty"));
        }

        try {
            Map<String, Object> marketData = pricingEngine.getMarketDataRaw(ticker);
            if (marketData == null || marketData.isEmpty()) {
                return ResponseEntity.status(502).body(Map.of("error", "No data received from market API"));
            }
            return ResponseEntity.ok(marketData);
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of("error", "Market API error: " + e.getMessage()));
        }
    }

    @GetMapping("/quotes")
    public ResponseEntity<?> getPrices(@RequestParam String symbols) {
        List<String> requestedSymbols = Arrays.stream(symbols.split(","))
                .map(String::trim)
                .filter(symbol -> !symbol.isEmpty())
                .toList();

        if (requestedSymbols.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Symbols cannot be empty"));
        }

        try {
            return ResponseEntity.ok(pricingEngine.getPrices(requestedSymbols));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of("error", "Market API error: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Pricing Engine is running");
    }
}
