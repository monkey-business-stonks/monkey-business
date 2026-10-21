package domain.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.client.RestTemplate;

import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.JWKSet;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${jwt.signing-key}")
    private String jwtSigningKey;

    @Value("${auth.service-url:http://localhost:3000}")
    private String authServiceUrl;

    // Note: restTemplate() @Bean removed from here to prevent collision with RestTemplateConfig.class

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

        grantedAuthoritiesConverter.setAuthoritiesClaimName("role"); 
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    @Bean
    public JwtDecoder jwtDecoder(RestTemplate restTemplate) {
        var key = new SecretKeySpec(
            jwtSigningKey.getBytes(StandardCharsets.UTF_8),
            "HmacSHA256"
        );

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).build();

        OAuth2TokenValidator<Jwt> revocationValidator = jwt -> {
            String jti = jwt.getId();
            if (jti == null) {
                return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Missing JWT JTI claim", null)
                );
            }

            try {
                Map<String, Object> response = restTemplate.postForObject(
                    authServiceUrl + "/auth/verify-status",
                    Map.of("jti", jti), 
                    Map.class
                );

                if (response != null && Boolean.FALSE.equals(response.get("active"))) {
                    return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("token_revoked", "Token has been revoked", null)
                    );
                }
            } catch (Exception e) {
                return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("auth_error", "Failed to check token revocation status", null)
                );
            }

            return OAuth2TokenValidatorResult.success();
        };

        OAuth2TokenValidator<Jwt> combinedValidator = new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(),
            revocationValidator
        );

        decoder.setJwtValidator(combinedValidator);
        return decoder;
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        var secretKey = new OctetSequenceKey.Builder(
            jwtSigningKey.getBytes(StandardCharsets.UTF_8)
        ).build();
        var jwkSet = new ImmutableJWKSet<>(new JWKSet(secretKey));
        return new NimbusJwtEncoder(jwkSet);
    }

    // Filter chain for PUBLIC endpoints 
    @Bean
    @Order(1)
        public SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
            http
                .securityMatcher(
                    "/users",
                    "/users/authenticate",
                    "/users/refresh",
                    "/users/health",
                    "/market/health",
                    "/ws",
                    "/ws/**",
                    "/market/price/**",
                    "/market/quotes",
                    "/market/candles/**"
                )
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
    }

    // Filter chain for PROTECTED endpoints with OAuth2
    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http, RestTemplate restTemplate) throws Exception {
        http
            .cors(cors -> {})
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder(restTemplate))
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/ws", "/ws/**").permitAll()
                .anyRequest().authenticated());

        return http.build();
    }
}