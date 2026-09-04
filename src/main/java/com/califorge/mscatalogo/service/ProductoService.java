package com.califorge.mscatalogo.service;

import com.califorge.mscatalogo.dto.ProductoRequest;
import com.califorge.mscatalogo.model.Producto;
import com.califorge.mscatalogo.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public Producto crear(ProductoRequest request) {
        throw new UnsupportedOperationException("Scaffolding: metodo no implementado");
    }

    @Transactional(readOnly = true)
    public List<Producto> listar() {
        throw new UnsupportedOperationException("Scaffolding: metodo no implementado");
    }

    @Transactional(readOnly = true)
    public Optional<Producto> buscarPorId(Long id) {
        throw new UnsupportedOperationException("Scaffolding: metodo no implementado");
    }

    public Optional<Producto> actualizar(Long id, ProductoRequest request) {
        throw new UnsupportedOperationException("Scaffolding: metodo no implementado");
    }

    public Optional<Producto> eliminar(Long id) {
        throw new UnsupportedOperationException("Scaffolding: metodo no implementado");
    }
}