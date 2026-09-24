package com.calisat.mscatalogo.service;

import com.calisat.mscatalogo.dto.ProductoRequest;
import com.calisat.mscatalogo.exception.SkuActualizacionNoPermitidaException;
import com.calisat.mscatalogo.exception.SkuDuplicadoException;
import com.calisat.mscatalogo.model.Producto;
import com.calisat.mscatalogo.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ProductoService productoService;

    @Test
    void crear_mapeaCamposYActivaElProducto() {
        when(productoRepository.existsBySku("ANILLAS-001")).thenReturn(false);
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductoRequest request = new ProductoRequest(
                "ANILLAS-001", "Anillas de madera", "Set de anillas", new BigDecimal("19.99"), "Anillas", "");

        Producto resultado = productoService.crear(request);

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(productoRepository).save(captor.capture());
        Producto entidad = captor.getValue();
        assertEquals("ANILLAS-001", entidad.getSku());
        assertEquals("Anillas de madera", entidad.getNombre());
        assertEquals("Anillas", entidad.getCategoria());
        assertTrue(entidad.isActivo());
        assertEquals("ANILLAS-001", resultado.getSku());
    }

    @Test
    void crear_lanzaSkuDuplicado() {
        when(productoRepository.existsBySku("ANILLAS-001")).thenReturn(true);
        ProductoRequest request = new ProductoRequest(
                "ANILLAS-001", "Anillas de madera", "Set de anillas", new BigDecimal("19.99"), "Anillas", "");

        assertThrows(SkuDuplicadoException.class, () -> productoService.crear(request));
        verify(productoRepository).existsBySku("ANILLAS-001");
    }

    @Test
    void buscarPorSku_devuelveVacioSiElProductoEstaInactivo() {
        Producto inactivo = producto("ANILLAS-001", false);
        when(productoRepository.findBySku("ANILLAS-001")).thenReturn(Optional.of(inactivo));

        Optional<Producto> resultado = productoService.buscarPorSku("ANILLAS-001");

        assertFalse(resultado.isPresent());
    }

    @Test
    void buscarPorSku_devuelveElProductoSiEstaActivo() {
        Producto activo = producto("ANILLAS-001", true);
        when(productoRepository.findBySku("ANILLAS-001")).thenReturn(Optional.of(activo));

        Optional<Producto> resultado = productoService.buscarPorSku("ANILLAS-001");

        assertTrue(resultado.isPresent());
        assertEquals("ANILLAS-001", resultado.get().getSku());
    }

    @Test
    void actualizar_lanzaSkuActualizacionNoPermitidaSiElSkuDelBodyDistintoAlDelPath() {
        ProductoRequest request = new ProductoRequest(
                "OTRO-SKU", "Anillas de madera", "Set de anillas", new BigDecimal("19.99"), "Anillas", "");

        assertThrows(SkuActualizacionNoPermitidaException.class,
                () -> productoService.actualizar("ANILLAS-001", request));
    }

    @Test
    void eliminar_ponElProductoInactivo() {
        Producto producto = producto("ANILLAS-001", true);
        when(productoRepository.findBySku("ANILLAS-001")).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Producto> resultado = productoService.eliminar("ANILLAS-001");

        assertTrue(resultado.isPresent());
        assertFalse(resultado.get().isActivo());
    }

    @Test
    void listarPorCategoria_devuelveLaPaginaDelRepository() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("id"));
        Page<Producto> pagina = new PageImpl<>(List.of(producto("ANILLAS-001", true)));
        when(productoRepository.findByCategoriaAndActivoTrue(eq("Anillas"), eq(pageable)))
                .thenReturn(pagina);

        Page<Producto> resultado = productoService.listarPorCategoria("Anillas", pageable);

        assertEquals(1, resultado.getTotalElements());
        assertEquals("ANILLAS-001", resultado.getContent().get(0).getSku());
        verify(productoRepository).findByCategoriaAndActivoTrue("Anillas", pageable);
    }

    @Test
    void listarInactivos_devuelveSoloLosInactivos() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Producto> pagina = new PageImpl<>(List.of(producto("ANILLAS-001", false)));
        when(productoRepository.findByActivoFalse(pageable)).thenReturn(pagina);

        Page<Producto> resultado = productoService.listarInactivos(pageable);

        assertEquals(1, resultado.getTotalElements());
        assertFalse(resultado.getContent().get(0).isActivo());
        verify(productoRepository).findByActivoFalse(pageable);
    }

    @Test
    void reactivar_ponElProductoActivo() {
        Producto inactivo = producto("ANILLAS-001", false);
        when(productoRepository.findBySku("ANILLAS-001")).thenReturn(Optional.of(inactivo));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Producto> resultado = productoService.reactivar("ANILLAS-001");

        assertTrue(resultado.isPresent());
        assertTrue(resultado.get().isActivo());
    }

    @Test
    void reactivar_devuelveVacioSiElSkuNoExiste() {
        when(productoRepository.findBySku("NO-EXISTE")).thenReturn(Optional.empty());

        Optional<Producto> resultado = productoService.reactivar("NO-EXISTE");

        assertFalse(resultado.isPresent());
    }

    private Producto producto(String sku, boolean activo) {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setSku(sku);
        producto.setNombre("Anillas de madera");
        producto.setPrecio(new BigDecimal("19.99"));
        producto.setCategoria("Anillas");
        producto.setActivo(activo);
        return producto;
    }
}
