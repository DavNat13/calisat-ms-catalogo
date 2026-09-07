package com.calisat.mscatalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductoRequest(
        @Schema(description = "SKU del producto (identificador canonico, unico).", example = "ANILLAS-001", maxLength = 64)
        @NotBlank(message = "sku es obligatorio")
        @Size(max = 64, message = "sku no puede superar 64 caracteres")
        String sku,

        @Schema(description = "Nombre del producto.", example = "Anillas de madera Pro", maxLength = 120)
        @NotBlank(message = "nombre es obligatorio")
        @Size(max = 120, message = "nombre no puede superar 120 caracteres")
        String nombre,

        @Schema(description = "Descripcion larga del producto.", maxLength = 1000)
        @Size(max = 1000, message = "descripcion no puede superar 1000 caracteres")
        String descripcion,

        @Schema(description = "Precio de venta (mayor a 0).", example = "19.99")
        @NotNull(message = "precio es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "precio debe ser mayor a 0")
        BigDecimal precio,

        @Schema(description = "Categoria del producto.", example = "Anillas", maxLength = 64)
        @NotBlank(message = "categoria es obligatoria")
        @Size(max = 64, message = "categoria no puede superar 64 caracteres")
        String categoria,

        @Schema(description = "URL de la imagen del producto (http/https/ftp). Opcional; vacio o null se acepta.", example = "https://img.test/anillas.jpg", maxLength = 500)
        @Pattern(regexp = "^(https?|ftp)://\\S+$|^$", message = "imagenUrl debe ser una URL http/https/ftp valida (o vacia)")
        @Size(max = 500, message = "imagenUrl no puede superar 500 caracteres")
        String imagenUrl) {
}
