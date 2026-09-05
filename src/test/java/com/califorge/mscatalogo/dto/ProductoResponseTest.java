package com.califorge.mscatalogo.dto;

import com.califorge.mscatalogo.model.Producto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductoResponseTest {

    @Test
    void desde_mapeaTodosLosCamposDeLaEntidad() {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setSku("ANILLAS-001");
        producto.setNombre("Anillas de Madera Pro");
        producto.setDescripcion("Detalles del equipamiento");
        producto.setPrecio(new BigDecimal("19.99"));
        producto.setCategoria("Anillas");
        producto.setImagenUrl("http://img.test/anillas.jpg");
        producto.setActivo(true);
        producto.setFechaCreacion(LocalDateTime.of(2026, 9, 4, 10, 30));

        ProductoResponse response = ProductoResponse.desde(producto);

        assertEquals(1L, response.id());
        assertEquals("ANILLAS-001", response.sku());
        assertEquals("Anillas de Madera Pro", response.nombre());
        assertEquals("Detalles del equipamiento", response.descripcion());
        assertEquals(new BigDecimal("19.99"), response.precio());
        assertEquals("Anillas", response.categoria());
        assertEquals("http://img.test/anillas.jpg", response.imagenUrl());
        assertTrue(response.activo());
        assertNotNull(response.fechaCreacion());
    }
}