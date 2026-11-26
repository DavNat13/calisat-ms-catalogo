package com.calisat.mscatalogo.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ALLOWED_ORIGIN =
            "https://ezeh839whh.execute-api.us-east-1.amazonaws.com";

    /** Conversor de roles (Azure y Cognito) de {@link CalisatJwtAuthenticationConverter}. */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(
            CalisatJwtAuthenticationConverter roleConverter) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roleConverter);
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(ALLOWED_ORIGIN));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.GET, "/api/v1/catalogo/inactivos").hasAnyRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.GET, "/api/v1/catalogo/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/catalogo", "/api/v1/catalogo/**").hasAnyRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/api/v1/catalogo", "/api/v1/catalogo/**").hasAnyRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/catalogo", "/api/v1/catalogo/**").hasAnyRole("ADMINISTRADOR")
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder)
                    .jwtAuthenticationConverter(jwtAuthenticationConverter))
            );
        return http.build();
    }
}
