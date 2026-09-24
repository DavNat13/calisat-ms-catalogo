package com.calisat.mscatalogo.controller;

import com.calisat.mscatalogo.dto.ProductoRequest;
import com.calisat.mscatalogo.exception.GlobalExceptionHandler;
import com.calisat.mscatalogo.exception.SkuDuplicadoException;
import com.calisat.mscatalogo.model.Producto;
import com.calisat.mscatalogo.service.ProductoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba unitaria del ProductoController usando Mockito para el servicio y MockMvc
 * standalone (en Spring Boot 4.x no existe @WebMvcTest; se usa standaloneSetup).
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
        mockMvc = MockMvcBuilders.standaloneSetup(productoController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listar_devuelvePaginaConContent() throws Exception {
        when(productoService.listar(any()))
                .thenReturn(pagina(producto("ANILLAS-001", true)));

        mockMvc.perform(get("/api/v1/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].sku", is("ANILLAS-001")))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    void buscarPorSku_devuelve200() throws Exception {
        when(productoService.buscarPorSku("ANILLAS-001"))
                .thenReturn(Optional.of(producto("ANILLAS-001", true)));

        mockMvc.perform(get("/api/v1/catalogo/{sku}", "ANILLAS-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku", is("ANILLAS-001")))
                .andExpect(jsonPath("$.nombre", is("Anillas de madera")));
    }

    @Test
    void buscarPorSku_devuelve404SiNoExiste() throws Exception {
        when(productoService.buscarPorSku("NO-EXISTE")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/catalogo/{sku}", "NO-EXISTE"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crear_devuelve201ConLocation() throws Exception {
        when(productoService.crear(any(ProductoRequest.class)))
                .thenAnswer(invocation -> producto("ANILLAS-001", true));

        String body = """
                {"sku":"ANILLAS-001","nombre":"Anillas de madera","descripcion":"Set de anillas",
                 "precio":19.99,"categoria":"Anillas","imagenUrl":"https://img.test/anillas.jpg"}
                """;

        mockMvc.perform(post("/api/v1/catalogo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", containsString("/api/v1/catalogo/ANILLAS-001")))
                .andExpect(jsonPath("$.sku", is("ANILLAS-001")))
                .andExpect(jsonPath("$.categoria", is("Anillas")));
    }

    @Test
    void crear_duplicado_devuelve400() throws Exception {
        when(productoService.crear(any(ProductoRequest.class)))
                .thenThrow(new SkuDuplicadoException("ANILLAS-001"));

        String body = """
                {"sku":"ANILLAS-001","nombre":"Anillas de madera","descripcion":"Set de anillas",
                 "precio":19.99,"categoria":"Anillas","imagenUrl":"https://img.test/anillas.jpg"}
                """;

        mockMvc.perform(post("/api/v1/catalogo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", is("Ya existe un Producto con el SKU: ANILLAS-001")));
    }

    @Test
    void listarPorCategoria_devuelvePaginaConContent() throws Exception {
        when(productoService.listarPorCategoria(eq("Anillas"), any()))
                .thenReturn(pagina(producto("ANILLAS-001", true)));

        mockMvc.perform(get("/api/v1/catalogo/categoria/{categoria}", "Anillas")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].sku", is("ANILLAS-001")))
                .andExpect(jsonPath("$.content[0].categoria", is("Anillas")))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    void listarInactivos_devuelvePaginaConContent() throws Exception {
        when(productoService.listarInactivos(any()))
                .thenReturn(pagina(producto("ANILLAS-001", false)));

        mockMvc.perform(get("/api/v1/catalogo/inactivos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].sku", is("ANILLAS-001")))
                .andExpect(jsonPath("$.content[0].activo", is(false)))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    void reactivar_devuelve200ConMensaje() throws Exception {
        when(productoService.reactivar("ANILLAS-001"))
                .thenReturn(Optional.of(producto("ANILLAS-001", true)));

        mockMvc.perform(post("/api/v1/catalogo/{sku}/reactivar", "ANILLAS-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku", is("ANILLAS-001")))
                .andExpect(jsonPath("$.mensaje", is("Producto reactivado correctamente")));
    }

    @Test
    void reactivar_devuelve404SiElSkuNoExiste() throws Exception {
        when(productoService.reactivar("NO-EXISTE")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/catalogo/{sku}/reactivar", "NO-EXISTE"))
                .andExpect(status().isNotFound());
    }

    private PageImpl<Producto> pagina(Producto... productos) {
        // PageRequest (no Unpaged): Unpaged.getOffset() lanza UnsupportedOperationException al serializar
        return new PageImpl<>(List.of(productos), PageRequest.of(0, 20), productos.length);
    }

    private Producto producto(String sku, boolean activo) {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setSku(sku);
        producto.setNombre("Anillas de madera");
        producto.setDescripcion("Set de anillas");
        producto.setPrecio(new BigDecimal("19.99"));
        producto.setCategoria("Anillas");
        producto.setImagenUrl("https://img.test/anillas.jpg");
        producto.setActivo(activo);
        return producto;
    }
}
