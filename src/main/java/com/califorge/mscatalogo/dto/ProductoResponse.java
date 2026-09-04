package com.califorge.mscatalogo.dto;

import com.califorge.mscatalogo.model.Producto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductoResponse(
        Long id,
        String sku,
        String nombre,
        String descripcion,
        BigDecimal precio,
        String categoria,
        String imagenUrl,
        boolean activo,
        LocalDateTime fechaCreacion) {

    public static ProductoResponse desde(Producto p) {
        return new ProductoResponse(
                p.getId(),
                p.getSku(),
                p.getNombre(),
                p.getDescripcion(),
                p.getPrecio(),
                p.getCategoria(),
                p.getImagenUrl(),
                p.isActivo(),
                p.getFechaCreacion());
    }
}