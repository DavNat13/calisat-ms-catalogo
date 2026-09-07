package com.calisat.mscatalogo.exception;

public class SkuDuplicadoException extends RuntimeException {

    public SkuDuplicadoException(String sku) {
        super("Ya existe un Producto con el SKU: " + sku);
    }
}
