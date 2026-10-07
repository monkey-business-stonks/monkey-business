package domain.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    
    /**
     * Configure RestTemplate for HTTP calls to external APIs (real-time market data)
     * - Connection timeout: 5 seconds (fail fast if API is unreachable)
     * - Read timeout: 10 seconds (real-time requirement)
     * If timeouts occur, MarketDataService falls back to cached/mock prices
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(java.time.Duration.ofSeconds(5))
                .setReadTimeout(java.time.Duration.ofSeconds(10))
                .build();
    }
}
