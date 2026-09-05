package com.califorge.mscatalogo.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de la Fase 8 (Endurecer seguridad) ejecutando la SecurityFilterChain real.
 * Carga solo la config de seguridad (sin JPA/BD). Las propiedades JWT son ficticias;
 * la red JWKS solo se consultaria al validar un token real, no en requests sin
 * header Authorization.
 *
 * El contrato de calisat-ms-catalogo (ESP.md) hace publica la lectura del catalogo
 * (GET /api/v1/catalogo/** sin token) y protege la administracion (POST/PUT/DELETE).
 *
 * Se usa un controller de prueba que responde 200 en cualquier ruta, para
 * distinguir "rechazado por falta de autenticacion" (401) de "404".
 */
@SpringJUnitConfig
@WebAppConfiguration
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://example.com/",
        "spring.security.oauth2.resourceserver.jwt.tenant-id=fake-tenant",
        "CORS_ALLOWED_ORIGINS=http://localhost:5173"
})
@Import({SecurityConfig.class, SecurityConfigTest.TestControllerConfig.class})
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void health_esAccesibleSinAutenticacion() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorInfo_requiereAutenticacion() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lecturaDelCatalogo_esPublicaSinToken() throws Exception {
        mockMvc.perform(get("/api/v1/catalogo"))
                .andExpect(status().isOk());
    }

    @Test
    void detallePorSku_esPublicoSinToken() throws Exception {
        mockMvc.perform(get("/api/v1/catalogo/ANILLAS-001"))
                .andExpect(status().isOk());
    }

    @Test
    void crearProducto_requiereAutenticacion() throws Exception {
        mockMvc.perform(post("/api/v1/catalogo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void eliminarProducto_requiereAutenticacion() throws Exception {
        mockMvc.perform(delete("/api/v1/catalogo/1"))
                .andExpect(status().isUnauthorized());
    }

    @Configuration
    @RestController
    static class TestControllerConfig {
        @GetMapping("/**")
        public ResponseEntity<String> any() {
            return ResponseEntity.ok("ok");
        }
    }
}