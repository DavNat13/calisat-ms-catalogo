package com.califorge.mscatalogo.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fail-fast de config JWT: sin audiencia/issuer/tenant, el MS debe negarse a
 * arrancar (C3). Con JWT_AUDIENCE vacio, el JwtAudienceValidator exigiria
 * aud "api://" y toda escritura respondería 401 silencioso en produccion.
 */
class SecurityConfigJwtTest {

    @Test
    void sinConfiguracionJwt_detectaTodasLasVariables() {
        List<String> faltantes = SecurityConfig.configJwtFaltante("", "", "");

        assertEquals(List.of("JWT_ISSUER_URI", "JWT_TENANT_ID", "JWT_AUDIENCE"), faltantes);
    }

    @Test
    void conAudienciaVacia_detectaSoloJwtAudiencia() {
        List<String> faltantes = SecurityConfig.configJwtFaltante("https://issuer.test", "tenant-1", "");

        assertEquals(List.of("JWT_AUDIENCE"), faltantes);
    }

    @Test
    void conConfiguracionCompleta_noDetectaFaltantes() {
        List<String> faltantes = SecurityConfig.configJwtFaltante("https://issuer.test", "tenant-1", "calisat-app");

        assertTrue(faltantes.isEmpty());
    }
}