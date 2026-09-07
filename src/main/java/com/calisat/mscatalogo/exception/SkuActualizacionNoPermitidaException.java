package com.calisat.mscatalogo.exception;

public class SkuActualizacionNoPermitidaException extends RuntimeException {

    public SkuActualizacionNoPermitidaException(String sku) {
        super("El SKU no puede modificarse; el identificador canonico del producto es inmutable: " + sku);
    }
}
