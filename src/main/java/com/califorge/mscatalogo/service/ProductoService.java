package com.califorge.mscatalogo.service;

import com.califorge.mscatalogo.dto.ProductoRequest;
import com.califorge.mscatalogo.exception.SkuDuplicadoException;
import com.califorge.mscatalogo.model.Producto;
import com.califorge.mscatalogo.repository.ProductoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Reglas de negocio del catálogo (ESP.md):
 * 1. Baja lógica: eliminar solo cambia {@code activo} a false, nunca borra fisicamente.
 * 2. Filtros públicos: las consultas GET solo devuelven productos activos.
 * 3. Unicidad: no pueden existir dos productos con el mismo SKU.
 *
 * Nota sobre concurrencia: el check {@code existsBySku} es una defensa temprana
 * para responder 400 rapido, NO una garantia de unicidad. La garantia real la pone
 * la constraint UNIQUE del SKU en BD (DataIntegrityViolationException -> 400 en el
 * GlobalExceptionHandler) en escenarios TOCTOU con requests simultaneos.
 */
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

    /**
     * Actualiza los campos editables del producto. Semantica REPLACE: el cliente envia
     * el estado completo (sku, nombre, descripcion, precio, categoria, imagenUrl).
     * El SKU nuevo debe ser unico: si pertenece a otro producto, se rechaza con 400.
     */
    public Optional<Producto> actualizar(Long id, ProductoRequest request) {
        if (request.sku() != null && productoRepository.existsBySku(request.sku())) {
            Optional<Producto> mismo = productoRepository.findBySku(request.sku());
            if (mismo.isEmpty() || !mismo.get().getId().equals(id)) {
                throw new SkuDuplicadoException(request.sku());
            }
        }
        return productoRepository.findById(id)
                .map(producto -> {
                    producto.setSku(request.sku());
                    producto.setNombre(request.nombre());
                    producto.setDescripcion(request.descripcion());
                    producto.setPrecio(request.precio());
                    producto.setCategoria(request.categoria());
                    producto.setImagenUrl(request.imagenUrl());
                    return productoRepository.save(producto);
                });
    }

    /**
     * Baja logica (regla 1 de la spec): marca {@code activo=false} y guarda.
     * No se invoca {@code delete} del repositorio para preservar el historial.
     */
    public Optional<Producto> eliminar(Long id) {
        return productoRepository.findById(id)
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