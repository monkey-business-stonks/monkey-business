package domain.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.annotation.Order;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.JWKSet;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * JWT Security Configuration for OAuth2 Resource Server.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${jwt.signing-key}")
    private String jwtSigningKey;

    @Bean
    public JwtDecoder jwtDecoder() {
        var key = new SecretKeySpec(
                jwtSigningKey.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        var decoder = NimbusJwtDecoder.withSecretKey(key).build();

        return decoder;
    }

    /**
     * Creates a JWT encoder that signs tokens using HMAC-SHA256.
     */
    @Bean
    public JwtEncoder jwtEncoder() {
        var secretKey = new OctetSequenceKey.Builder(
                jwtSigningKey.getBytes(StandardCharsets.UTF_8)
        ).build();
        var jwkSet = new ImmutableJWKSet<>(new JWKSet(secretKey));
        return new NimbusJwtEncoder(jwkSet);
    }

     /**
     * Configures a separate security filter chain for public endpoints.
     * This filter chain is evaluated FIRST (@Order(1)) to allow public endpoints
     * to bypass JWT authentication entirely.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
        http
                // Match all public endpoints
                .securityMatcher(
                        "/users",
                        "/users/authenticate",
                        "/users/health",
                        "/users/*/accounts",
                        "/users/*/accounts/*",
                        "/market/health",
                        "/market/price/**",
                        "/accounts/*/orders"
                )
                // Disable CSRF for public endpoints
                .csrf(csrf -> csrf.disable())
                // Session configuration
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // Authorization rules for public endpoints
                .authorizeHttpRequests(auth -> auth
                        // Allow all public endpoints without authentication
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    /**
     * Configures the HTTP security filter chain for JWT-based authentication.
     * This filter chain is evaluated SECOND (@Order(2)) and protects all remaining endpoints.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
