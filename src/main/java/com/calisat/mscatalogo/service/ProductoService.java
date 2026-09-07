package com.calisat.mscatalogo.service;

import com.calisat.mscatalogo.dto.ProductoRequest;
import com.calisat.mscatalogo.exception.SkuActualizacionNoPermitidaException;
import com.calisat.mscatalogo.exception.SkuDuplicadoException;
import com.calisat.mscatalogo.model.Producto;
import com.calisat.mscatalogo.repository.ProductoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        if (productoRepository.existsBySku(request.sku())) {
            throw new SkuDuplicadoException(request.sku());
        }
        return productoRepository.save(mapearParaGrabar(request));
    }

    @Transactional(readOnly = true)
    public Page<Producto> listar(Pageable pageable) {
        return productoRepository.findByActivoTrue(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Producto> buscarPorSku(String sku) {
        return productoRepository.findBySku(sku).filter(Producto::isActivo);
    }

    @Transactional(readOnly = true)
    public List<Producto> listarPorCategoria(String categoria) {
        return productoRepository.findByCategoriaAndActivoTrue(categoria);
    }

    public Optional<Producto> actualizar(String sku, ProductoRequest request) {
        if (request.sku() != null && !request.sku().equals(sku)) {
            throw new SkuActualizacionNoPermitidaException(sku);
        }
        return productoRepository.findBySku(sku)
                .filter(Producto::isActivo)
                .map(producto -> {
                    producto.setNombre(request.nombre());
                    producto.setDescripcion(request.descripcion());
                    producto.setPrecio(request.precio());
                    producto.setCategoria(request.categoria());
                    producto.setImagenUrl(request.imagenUrl());
                    return productoRepository.save(producto);
                });
    }

    public Optional<Producto> eliminar(String sku) {
        return productoRepository.findBySku(sku)
                .map(producto -> {
                    producto.setActivo(false);
                    return productoRepository.save(producto);
                });
    }

    private Producto mapearParaGrabar(ProductoRequest request) {
        Producto producto = new Producto();
        producto.setSku(request.sku());
        producto.setNombre(request.nombre());
        producto.setDescripcion(request.descripcion());
        producto.setPrecio(request.precio());
        producto.setCategoria(request.categoria());
        producto.setImagenUrl(request.imagenUrl());
        producto.setActivo(true);
        return producto;
    }
}
