package com.califorge.mscatalogo.controller;

import com.califorge.mscatalogo.dto.ProductoRequest;
import com.califorge.mscatalogo.exception.GlobalExceptionHandler;
import com.califorge.mscatalogo.exception.SkuDuplicadoException;
import com.califorge.mscatalogo.model.Producto;
import com.califorge.mscatalogo.service.ProductoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.SpringDataJackson3Configuration;
import org.springframework.data.web.config.SpringDataWebSettings;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba unitaria del ProductoController usando Mockito para el servicio y MockMvc
 * standalone (en Spring Boot 4.x no existe @WebMvcTest; se usa standaloneSetup).
 * Se registra el GlobalExceptionHandler para cubrir el mapeo 400 de SKU duplicado.
 */
@ExtendWith(MockitoExtension.class)
class ProductoControllerTest {

    @Mock
    private ProductoService productoService;

    @InjectMocks
    private ProductoController productoController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        JsonMapper mapper = JsonMapper.builder()
                .addModule(new SpringDataJackson3Configuration.PageModule(
                        new SpringDataWebSettings(EnableSpringDataWebSupport.PageSerializationMode.DIRECT)))
                .build();
        mockMvc = MockMvcBuilders.standaloneSetup(productoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(mapper))
                .build();
    }

    @Test
    void listar_devuelveProductosActivosPaginados() throws Exception {
        Producto producto = producto(1L, "ANILLAS-001");
        when(productoService.listar(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(producto)));

        mockMvc.perform(get("/api/v1/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].sku", is("ANILLAS-001")))
                .andExpect(jsonPath("$.content[0].precio", is(19.99)));
    }

    @Test
    void buscarPorSku_devuelveProducto() throws Exception {
        Producto producto = producto(1L, "ANILLAS-001");
        when(productoService.buscarPorSku("ANILLAS-001")).thenReturn(Optional.of(producto));

        mockMvc.perform(get("/api/v1/catalogo/{sku}", "ANILLAS-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.sku", is("ANILLAS-001")))
                .andExpect(jsonPath("$.activo", is(true)));
    }

    @Test
    void buscarPorSku_devuelve404SiNoExiste() throws Exception {
        when(productoService.buscarPorSku("NO-EXISTE")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/catalogo/{sku}", "NO-EXISTE"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarPorCategoria_devuelveSoloActivos() throws Exception {
        Producto producto = producto(1L, "ANILLAS-001");
        when(productoService.listarPorCategoria("Anillas")).thenReturn(List.of(producto));

        mockMvc.perform(get("/api/v1/catalogo/categoria/{categoria}", "Anillas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sku", is("ANILLAS-001")));
    }

    @Test
    void crear_devuelve201ConLocation() throws Exception {
        Producto guardado = producto(1L, "ANILLAS-001");
        when(productoService.crear(any(ProductoRequest.class))).thenReturn(guardado);

        String body = """
                {"sku":"ANILLAS-001","nombre":"Anillas de Madera Pro","descripcion":"Detalles del equipamiento","precio":19.99,"categoria":"Anillas","imagenUrl":"http://img.test/anillas.jpg"}
                """;

        mockMvc.perform(post("/api/v1/catalogo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("/api/v1/catalogo/ANILLAS-001")))
                .andExpect(jsonPath("$.sku", is("ANILLAS-001")))
                .andExpect(jsonPath("$.activo", is(true)));
    }

    @Test
    void crear_devuelve400SiElSkuYaExiste() throws Exception {
        when(productoService.crear(any(ProductoRequest.class)))
                .thenThrow(new SkuDuplicadoException("ANILLAS-001"));

        String body = """
                {"sku":"ANILLAS-001","nombre":"Anillas de Madera Pro","precio":19.99,"categoria":"Anillas"}
                """;

        mockMvc.perform(post("/api/v1/catalogo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", is("Ya existe un Producto con el SKU: ANILLAS-001")));
    }

    @Test
    void crear_conJsonMalformado_devuelve400No500() throws Exception {
        String body = "{ \"sku\": \"ANILLAS-001\", \"nombre\": \"incompleto";

        mockMvc.perform(post("/api/v1/catalogo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void actualizar_conIdNoNumerico_devuelve400No500() throws Exception {
        mockMvc.perform(put("/api/v1/catalogo/{id}", "abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"ANILLAS-002","nombre":"Anillas Pro 2","precio":29.99,"categoria":"Anillas"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void actualizar_devuelve200() throws Exception {
        Producto actualizado = producto(1L, "ANILLAS-002");
        when(productoService.actualizar(eq(1L), any(ProductoRequest.class)))
                .thenReturn(Optional.of(actualizado));

        String body = """
                {"sku":"ANILLAS-002","nombre":"Anillas Pro 2","precio":29.99,"categoria":"Anillas"}
                """;

        mockMvc.perform(put("/api/v1/catalogo/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku", is("ANILLAS-002")));
    }

    @Test
    void actualizar_devuelve404SiNoExiste() throws Exception {
        when(productoService.actualizar(eq(99L), any(ProductoRequest.class)))
                .thenReturn(Optional.empty());

        String body = """
                {"sku":"ANILLAS-002","nombre":"Anillas Pro 2","precio":29.99,"categoria":"Anillas"}
                """;

        mockMvc.perform(put("/api/v1/catalogo/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminar_devuelve200() throws Exception {
        Producto producto = producto(1L, "ANILLAS-001");
        producto.setActivo(false);
        when(productoService.eliminar(1L)).thenReturn(Optional.of(producto));

        mockMvc.perform(delete("/api/v1/catalogo/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje", is("Producto dado de baja correctamente")));
    }

    @Test
    void eliminar_devuelve404SiNoExiste() throws Exception {
        when(productoService.eliminar(99L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/v1/catalogo/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    private Producto producto(Long id, String sku) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setSku(sku);
        producto.setNombre("Anillas de Madera Pro");
        producto.setDescripcion("Detalles del equipamiento");
        producto.setPrecio(new BigDecimal("19.99"));
        producto.setCategoria("Anillas");
        producto.setImagenUrl("http://img.test/anillas.jpg");
        producto.setActivo(true);
        producto.setFechaCreacion(LocalDateTime.of(2026, 9, 4, 10, 30));
        return producto;
    }
}