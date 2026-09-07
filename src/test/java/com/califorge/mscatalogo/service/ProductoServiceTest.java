package com.califorge.mscatalogo.service;

import com.califorge.mscatalogo.dto.ProductoRequest;
import com.califorge.mscatalogo.exception.SkuActualizacionNoPermitidaException;
import com.califorge.mscatalogo.exception.SkuDuplicadoException;
import com.califorge.mscatalogo.model.Producto;
import com.califorge.mscatalogo.repository.ProductoRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ProductoService productoService;

    @Test
    void crear_mapeaCamposDeProducto() {
        when(productoRepository.existsBySku("ANILLAS-001")).thenReturn(false);
        Producto guardado = new Producto();
        guardado.setId(1L);
        when(productoRepository.save(any(Producto.class))).thenReturn(guardado);

        ProductoRequest request = request("ANILLAS-001");

        productoService.crear(request);

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(productoRepository).save(captor.capture());
        Producto entidad = captor.getValue();
        assertEquals("ANILLAS-001", entidad.getSku());
        assertEquals("Anillas de Madera Pro", entidad.getNombre());
        assertEquals("Detalles del equipamiento", entidad.getDescripcion());
        assertEquals(new BigDecimal("19.99"), entidad.getPrecio());
        assertEquals("Anillas", entidad.getCategoria());
        assertEquals("http://img.test/anillas.jpg", entidad.getImagenUrl());
        assertTrue(entidad.isActivo());
    }

    @Test
    void crear_lanzaSkuDuplicado() {
        when(productoRepository.existsBySku("ANILLAS-001")).thenReturn(true);
        ProductoRequest request = request("ANILLAS-001");

        assertThrows(SkuDuplicadoException.class, () -> productoService.crear(request));
        verify(productoRepository).existsBySku("ANILLAS-001");
    }

    @Test
    void listar_devuelveSoloActivosPaginados() {
        Producto producto = producto("ANILLAS-001", true);
        Pageable pageable = PageRequest.of(0, 20);
        when(productoRepository.findByActivoTrue(pageable))
                .thenReturn(new PageImpl<>(List.of(producto)));

        Page<Producto> resultado = productoService.listar(pageable);

        assertEquals(1, resultado.getContent().size());
        assertEquals("ANILLAS-001", resultado.getContent().get(0).getSku());
        verify(productoRepository).findByActivoTrue(pageable);
    }

    @Test
    void buscarPorSku_devuelveProductoActivo() {
        Producto producto = producto("ANILLAS-001", true);
        when(productoRepository.findBySku("ANILLAS-001")).thenReturn(Optional.of(producto));

        Optional<Producto> resultado = productoService.buscarPorSku("ANILLAS-001");

        assertTrue(resultado.isPresent());
        assertEquals("ANILLAS-001", resultado.get().getSku());
    }

    @Test
    void buscarPorSku_ignoraProductoInactivo() {
        Producto producto = producto("ANILLAS-001", false);
        when(productoRepository.findBySku("ANILLAS-001")).thenReturn(Optional.of(producto));

        Optional<Producto> resultado = productoService.buscarPorSku("ANILLAS-001");

        assertFalse(resultado.isPresent());
    }

    @Test
    void buscarPorSku_devuelveVacioSiNoExiste() {
        when(productoRepository.findBySku("NO-EXISTE")).thenReturn(Optional.empty());

        Optional<Producto> resultado = productoService.buscarPorSku("NO-EXISTE");

        assertFalse(resultado.isPresent());
    }

    @Test
    void listarPorCategoria_devuelveSoloActivosDeLaCategoria() {
        Producto activo = producto("ANILLAS-001", true);
        when(productoRepository.findByCategoriaAndActivoTrue("Anillas")).thenReturn(List.of(activo));

        List<Producto> resultado = productoService.listarPorCategoria("Anillas");

        assertEquals(1, resultado.size());
        assertEquals("ANILLAS-001", resultado.get(0).getSku());
        verify(productoRepository).findByCategoriaAndActivoTrue("Anillas");
    }

    @Test
    void actualizar_reescribeCampos() {
        Producto existente = producto("ANILLAS-001", true);
        existente.setId(1L);
        when(productoRepository.findBySku("ANILLAS-001")).thenReturn(Optional.of(existente));
        when(productoRepository.save(any(Producto.class))).thenReturn(existente);

        ProductoRequest request = new ProductoRequest(
                "ANILLAS-001", "Anillas Pro 2", "Nueva descripcion",
                new BigDecimal("29.99"), "Anillas", "http://img.test/anillas2.jpg");

        Optional<Producto> resultado = productoService.actualizar("ANILLAS-001", request);

        assertTrue(resultado.isPresent());
        assertEquals("ANILLAS-001", resultado.get().getSku());
        assertEquals("Anillas Pro 2", resultado.get().getNombre());
        assertEquals("Nueva descripcion", resultado.get().getDescripcion());
        assertEquals(new BigDecimal("29.99"), resultado.get().getPrecio());
        assertEquals("Anillas", resultado.get().getCategoria());
        assertEquals("http://img.test/anillas2.jpg", resultado.get().getImagenUrl());
        verify(productoRepository).findBySku("ANILLAS-001");
        verify(productoRepository, never()).existsBySku(any());
    }

    @Test
    void actualizar_rechazaCambioDeSku() {
        ProductoRequest request = new ProductoRequest(
                "ANILLAS-002", "Anillas Pro 2", "Nueva descripcion",
                new BigDecimal("29.99"), "Anillas", null);

        assertThrows(SkuActualizacionNoPermitidaException.class,
                () -> productoService.actualizar("ANILLAS-001", request));
        verify(productoRepository, never()).existsBySku(any());
    }

    @Test
    void actualizar_conElMismoSkuNoRechazaDuplicado() {
        Producto existente = producto("ANILLAS-001", true);
        existente.setId(1L);
        when(productoRepository.findBySku("ANILLAS-001")).thenReturn(Optional.of(existente));
        when(productoRepository.save(any(Producto.class))).thenReturn(existente);

        ProductoRequest request = request("ANILLAS-001");

        Optional<Producto> resultado = productoService.actualizar("ANILLAS-001", request);

        assertTrue(resultado.isPresent());
        verify(productoRepository, never()).existsBySku("ANILLAS-001");
    }

    @Test
    void actualizar_devuelveVacioSiNoExiste() {
        when(productoRepository.findBySku("NO-EXISTE")).thenReturn(Optional.empty());

        ProductoRequest request = request("NO-EXISTE");

        Optional<Producto> resultado = productoService.actualizar("NO-EXISTE", request);

        assertFalse(resultado.isPresent());
    }

    @Test
    void actualizar_noActualizaProductoInactivo() {
        Producto inactivo = producto("ANILLAS-001", false);
        when(productoRepository.findBySku("ANILLAS-001")).thenReturn(Optional.of(inactivo));

        ProductoRequest request = request("ANILLAS-001");

        Optional<Producto> resultado = productoService.actualizar("ANILLAS-001", request);

        assertFalse(resultado.isPresent());
        verify(productoRepository, never()).save(any(Producto.class));
    }

    @Test
    void eliminar_haceBajaLogicaYNoBorraFisico() {
        Producto producto = producto("ANILLAS-001", true);
        producto.setId(1L);
        when(productoRepository.findBySku("ANILLAS-001")).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenReturn(producto);

        Optional<Producto> resultado = productoService.eliminar("ANILLAS-001");

        assertTrue(resultado.isPresent());
        assertFalse(resultado.get().isActivo());
        verify(productoRepository).save(producto);
        verify(productoRepository, never()).delete(any(Producto.class));
    }

    @Test
    void eliminar_esIdempotenteSobreUnProductoYaInactivo() {
        Producto inactivo = producto("ANILLAS-001", false);
        when(productoRepository.findBySku("ANILLAS-001")).thenReturn(Optional.of(inactivo));
        when(productoRepository.save(any(Producto.class))).thenReturn(inactivo);

        Optional<Producto> resultado = productoService.eliminar("ANILLAS-001");

        assertTrue(resultado.isPresent());
        assertFalse(resultado.get().isActivo());
    }

    @Test
    void eliminar_devuelveVacioSiNoExiste() {
        when(productoRepository.findBySku("NO-EXISTE")).thenReturn(Optional.empty());

        Optional<Producto> resultado = productoService.eliminar("NO-EXISTE");

        assertFalse(resultado.isPresent());
    }

    private ProductoRequest request(String sku) {
        return new ProductoRequest(
                sku, "Anillas de Madera Pro", "Detalles del equipamiento",
                new BigDecimal("19.99"), "Anillas", "http://img.test/anillas.jpg");
    }

    private Producto producto(String sku, boolean activo) {
        Producto producto = new Producto();
        producto.setSku(sku);
        producto.setNombre("Anillas de Madera Pro");
        producto.setDescripcion("Detalles del equipamiento");
        producto.setPrecio(new BigDecimal("19.99"));
        producto.setCategoria("Anillas");
        producto.setImagenUrl("http://img.test/anillas.jpg");
        producto.setActivo(activo);
        producto.setFechaCreacion(LocalDateTime.of(2026, 9, 4, 10, 30));
        return producto;
    }
}