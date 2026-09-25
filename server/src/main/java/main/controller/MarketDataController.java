package main.controller;

import main.dto.MarketDataDto;
import main.service.MarketDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/market")
@CrossOrigin(origins = "*")
public class MarketDataController {

    @Autowired
    private MarketDataService marketDataService;

    /**
     * Get current market data for a ticker
     * @param ticker Stock ticker symbol (e.g., AAPL)
     * @return MarketDataDto with current price and timestamp
     */
    @GetMapping("/price/{ticker}")
    public ResponseEntity<MarketDataDto> getPrice(@PathVariable String ticker) {
        if (ticker == null || ticker.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        MarketDataDto marketData = marketDataService.getMarketData(ticker);
        return ResponseEntity.ok(marketData);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Market Data Service is running");
    }
}
