package com.califorge.mscatalogo.exception;

/**
 * El SKU es el identificador canonico e inmutable del producto. Rechaza intentos
 * de renombrarlo via un mismatch entre el {@code sku} del path y el del body,
 * evitando SKUs huerfanos en ms-inventario (Auditoria 8.1).
 */
public class SkuActualizacionNoPermitidaException extends RuntimeException {

    public SkuActualizacionNoPermitidaException(String sku) {
        super("El SKU no puede modificarse; el identificador canonico del producto es inmutable: " + sku);
    }
}
