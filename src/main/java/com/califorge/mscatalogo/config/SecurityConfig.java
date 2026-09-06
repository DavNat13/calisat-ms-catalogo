package com.califorge.mscatalogo.config;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Contrato de seguridad (ESP.md): la lectura del catalogo es publica para clientes;
 * los endpoints de administracion (POST/PUT/DELETE) exigen JWT valido.
 * Decision del equipo (revision 2026-09-05): "Administradores" en ESP se interpreta
 * como "usuario autenticado" (no se exige un claim de rol en esta iteracion).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.tenant-id}")
    private String tenantId;

    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:5173}")
    private String allowedOrigins;

    @Value("${JWT_AUDIENCE:}")
    private String jwtAudience;

    /**
     * Fail-fast (C3): si la config JWT llega vacia (por ejemplo, Compose
     * sustituye la variable faltante con ""), el MS debe negarse a arrancar en
     * lugar de responder 401 silencioso en toda escritura.
     */
    @PostConstruct
    void validarConfigJwt() {
        List<String> faltantes = configJwtFaltante(issuerUri, tenantId, jwtAudience);
        if (!faltantes.isEmpty()) {
            throw new IllegalStateException(
                    "Configuracion JWT incompleta en el arranque: faltan "
                            + String.join(", ", faltantes)
                            + ". Define JWT_ISSUER_URI, JWT_TENANT_ID y JWT_AUDIENCE en la instancia (docker-compose/.env) antes de desplegar.");
        }
    }

    static List<String> configJwtFaltante(String issuerUri, String tenantId, String jwtAudience) {
        List<String> faltantes = new ArrayList<>();
        if (esBlanco(issuerUri)) {
            faltantes.add("JWT_ISSUER_URI");
        }
        if (esBlanco(tenantId)) {
            faltantes.add("JWT_TENANT_ID");
        }
        if (esBlanco(jwtAudience)) {
            faltantes.add("JWT_AUDIENCE");
        }
        return faltantes;
    }

    private static boolean esBlanco(String valor) {
        return valor == null || valor.isBlank();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        String jwkSetUri = issuerUri + tenantId + "/discovery/v2.0/keys";
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> audienceValidator =
                new JwtAudienceValidator("api://" + jwtAudience);
        jwtDecoder.setJwtValidator(audienceValidator);

        return jwtDecoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // allowCredentials(true) es necesario: el frontend envia JWT en el header
        // Authorization via credentials:include; sin esto, el navegador bloquea el header.
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.GET, "/api/v1/catalogo/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder()))
            );
        return http.build();
    }
}