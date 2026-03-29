package imitationgame.chatservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.Arrays;
import java.util.List;

@Configuration
public class JwtConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        // Accept tokens issued by the custom Python auth-service
        List<String> allowedIssuers = Arrays.asList(
            "http://auth-service:8000",
            "http://localhost:8000"
        );

        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new MultiIssuerValidator(allowedIssuers)
        );

        decoder.setJwtValidator(validator);
        return decoder;
    }

    /**
     * Custom validator that accepts multiple issuers
     */
    private static class MultiIssuerValidator implements OAuth2TokenValidator<Jwt> {
        private final List<String> allowedIssuers;

        public MultiIssuerValidator(List<String> allowedIssuers) {
            this.allowedIssuers = allowedIssuers;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : null;
            if (issuer != null && allowedIssuers.contains(issuer)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                new org.springframework.security.oauth2.core.OAuth2Error(
                    "invalid_token",
                    "Invalid issuer: " + issuer,
                    null
                )
            );
        }
    }
}
