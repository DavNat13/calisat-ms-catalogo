package com.calisat.mscatalogo.dto;

import com.calisat.mscatalogo.model.Producto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductoResponse(
        @Schema(description = "Identificador interno de persistencia (no usado en rutas; el identificador canonico es el SKU).", example = "1")
        Long id,

        @Schema(description = "SKU del producto (identificador canonico del recurso).", example = "ANILLAS-001")
        String sku,

        @Schema(description = "Nombre del producto.")
        String nombre,

        @Schema(description = "Descripcion larga del producto.")
        String descripcion,

        @Schema(description = "Precio de venta (mayor a 0).")
        BigDecimal precio,

        @Schema(description = "Categoria del producto.")
        String categoria,

        @Schema(description = "URL de la imagen del producto (http/https).")
        String imagenUrl,

        @Schema(description = "Indica si el producto esta activo (baja logica).")
        boolean activo,

        @Schema(description = "Fecha de creacion del registro.")
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
