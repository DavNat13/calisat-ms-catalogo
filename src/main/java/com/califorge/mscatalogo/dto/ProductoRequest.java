package com.califorge.mscatalogo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO de entrada para crear/actualizar un producto.
 * Excluye id, activo y fechaCreacion (se gestionan internamente en la entidad).
 */
public record ProductoRequest(
        @NotBlank(message = "sku es obligatorio")
        @Size(max = 64, message = "sku no puede superar 64 caracteres")
        String sku,

        @NotBlank(message = "nombre es obligatorio")
        @Size(max = 120, message = "nombre no puede superar 120 caracteres")
        String nombre,

        @Size(max = 1000, message = "descripcion no puede superar 1000 caracteres")
        String descripcion,

        @NotNull(message = "precio es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "precio debe ser mayor a 0")
        BigDecimal precio,

        @NotBlank(message = "categoria es obligatoria")
        @Size(max = 64, message = "categoria no puede superar 64 caracteres")
        String categoria,

        @Size(max = 500, message = "imagenUrl no puede superar 500 caracteres")
        String imagenUrl) {
}