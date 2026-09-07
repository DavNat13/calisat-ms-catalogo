package com.califorge.mscatalogo;

import com.califorge.mscatalogo.model.Producto;
import com.califorge.mscatalogo.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ITs end-to-end de la Fase 6: validan contra PostgreSQL real (Testcontainers) lo que
 * los tests unitarios simulan con Mockito. Ligadas a la fase verify por el
 * maven-failsafe-plugin (sufijo *IT), nunca a surefire/package: el Dockerfile corre
 * {@code mvn clean package} dentro del stage build, donde no hay daemon de Docker.
 */
@SpringBootTest
@ActiveProfiles("it")
@Testcontainers
class ProductoCatalogIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Boot 4 no aplica el configurer de seguridad en @AutoConfigureMockMvc; se aplica
        // explicitamente para que @WithMockUser pueble el SecurityContext del filtro real.
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        productoRepository.deleteAll();
    }

    @Test
    @WithMockUser
    void post_devuelve201ConLocation_porSku_yGetLoDevuelve() throws Exception {
        String body = jsonProducto("SKU-E2E-001", "Anillas Pro", "19.99");

        mockMvc.perform(post("/api/v1/catalogo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        containsString("/api/v1/catalogo/SKU-E2E-001")))
                .andExpect(jsonPath("$.sku").value("SKU-E2E-001"))
                .andExpect(jsonPath("$.activo").value(true));

        mockMvc.perform(get("/api/v1/catalogo/{sku}", "SKU-E2E-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-E2E-001"))
                .andExpect(jsonPath("$.precio").value(19.99));
    }

    @Test
    @WithMockUser
    void segundoPost_conElMismoSku_devuelve400() throws Exception {
        String body = jsonProducto("SKU-DUP-001", "Anillas", "19.99");

        mockMvc.perform(post("/api/v1/catalogo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/catalogo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    /**
     * Garantia REAL de unicidad, no la defensa temprana del service ({@code existsBySku}):
     * se inserta el SKU duplicado directo por repositorio (sin pasar por ProductoService,
     * por lo que su check previo no interviene) y la constraint UNIQUE de la BD es quien
     * lo rechaza con DataIntegrityViolationException -> 400. Simula el escenario TOCTOU
     * de la nota del service.
     */
    @Test
    @WithMockUser
    void laConstraintUnica_deLaBd_rechazaElRegistroDuplicado() throws Exception {
        mockMvc.perform(post("/api/v1/catalogo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonProducto("SKU-DUP-002", "Anillas", "19.99")))
                .andExpect(status().isCreated());

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status ->
                productoRepository.saveAndFlush(producto("SKU-DUP-002"))))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(productoRepository.count()).isEqualTo(1);
        assertThat(productoRepository.findBySku("SKU-DUP-002")).isPresent();
    }

    @Test
    @WithMockUser
    void bajaLogica_deleteDevuelve200_ocultaDelGet_yPersisteInactivo() throws Exception {
        mockMvc.perform(post("/api/v1/catalogo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonProducto("SKU-BAJA-001", "Anillas", "19.99")))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/catalogo/{sku}", "SKU-BAJA-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-BAJA-001"))
                .andExpect(jsonPath("$.mensaje").value("Producto dado de baja correctamente"));

        mockMvc.perform(get("/api/v1/catalogo/{sku}", "SKU-BAJA-001"))
                .andExpect(status().isNotFound());

        Producto persistido = productoRepository.findBySku("SKU-BAJA-001").orElseThrow();
        assertThat(persistido.isActivo()).isFalse();
    }

    @Test
    void paginacion_pageSize_totalesCorrectos() throws Exception {
        for (int i = 1; i <= 5; i++) {
            productoRepository.save(producto("PAG-" + i));
        }

        mockMvc.perform(get("/api/v1/catalogo")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].sku").value("PAG-1"))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    @WithMockUser
    void put_porSku_actualizaYDevuelve200() throws Exception {
        mockMvc.perform(post("/api/v1/catalogo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonProducto("SKU-PUT-001", "Anillas", "19.99")))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/v1/catalogo/{sku}", "SKU-PUT-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonProducto("SKU-PUT-001", "Anillas Renovadas", "25.99")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-PUT-001"))
                .andExpect(jsonPath("$.nombre").value("Anillas Renovadas"))
                .andExpect(jsonPath("$.precio").value(25.99));
    }

    @Test
    @WithMockUser
    void put_porSkuInexistente_devuelve404() throws Exception {
        mockMvc.perform(put("/api/v1/catalogo/{sku}", "SKU-NO-EXISTE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonProducto("SKU-NO-EXISTE", "Anillas", "19.99")))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void delete_esIdempotente_elSegundoDeleteDevuelve200() throws Exception {
        mockMvc.perform(post("/api/v1/catalogo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonProducto("SKU-DEL-001", "Anillas", "19.99")))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/catalogo/{sku}", "SKU-DEL-001"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/catalogo/{sku}", "SKU-DEL-001"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/catalogo/{sku}", "SKU-DEL-001"))
                .andExpect(status().isNotFound());
    }

    @Test
    void post_sinAutenticacion_devuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/catalogo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonProducto("SKU-401-001", "Anillas", "19.99")))
                .andExpect(status().isUnauthorized());
    }

    private Producto producto(String sku) {
        Producto producto = new Producto();
        producto.setSku(sku);
        producto.setNombre("Anillas de Madera Pro");
        producto.setDescripcion("Detalles del equipamiento");
        producto.setPrecio(new BigDecimal("19.99"));
        producto.setCategoria("Anillas");
        producto.setImagenUrl("http://img.test/anillas.jpg");
        producto.setActivo(true);
        return producto;
    }

    private String jsonProducto(String sku, String nombre, String precio) {
        return """
                {"sku":"%s","nombre":"%s","descripcion":"Detalles del equipamiento","precio":%s,"categoria":"Anillas","imagenUrl":"http://img.test/anillas.jpg"}
                """.formatted(sku, nombre, precio);
    }
}