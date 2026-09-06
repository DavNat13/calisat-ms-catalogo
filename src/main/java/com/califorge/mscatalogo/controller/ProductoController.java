package com.califorge.mscatalogo.controller;

import com.califorge.mscatalogo.dto.ProductoRequest;
import com.califorge.mscatalogo.dto.ProductoResponse;
import com.califorge.mscatalogo.model.Producto;
import com.califorge.mscatalogo.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * API REST del catálogo (ESP.md): lectura pública para clientes y
 * administración (POST/PUT/DELETE) protegida por JWT vía resource server.
 */
@RestController
@RequestMapping("/api/v1/catalogo")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    /**
     * GET /api/v1/catalogo
     * Lista productos activos con paginacion.
     */
    @GetMapping
    public ResponseEntity<Page<ProductoResponse>> listar(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        Page<ProductoResponse> productos = productoService.listar(pageable)
                .map(ProductoResponse::desde);
        return ResponseEntity.ok(productos);
    }

    /**
     * GET /api/v1/catalogo/{sku}
     * Detalle por SKU. Devuelve 404 si no existe o está inactivo.
     */
    @GetMapping("/{sku}")
    public ResponseEntity<ProductoResponse> buscarPorSku(@PathVariable String sku) {
        return productoService.buscarPorSku(sku)
                .map(ProductoResponse::desde)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/catalogo/categoria/{categoria}
     * Filtra productos activos por categoría.
     */
    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<ProductoResponse>> listarPorCategoria(@PathVariable String categoria) {
        List<ProductoResponse> productos = productoService.listarPorCategoria(categoria).stream()
                .map(ProductoResponse::desde)
                .toList();
        return ResponseEntity.ok(productos);
    }

    /**
     * POST /api/v1/catalogo
     * Crea un producto. Devuelve 400 si el SKU ya existe, 201 en exito con Location.
     */
    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        Producto guardado = productoService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{sku}")
                .buildAndExpand(guardado.getSku())
                .toUri();
        return ResponseEntity.created(location).body(ProductoResponse.desde(guardado));
    }

    /**
     * PUT /api/v1/catalogo/{sku}
     * Actualiza por SKU (identificador unificado). Devuelve 404 si no existe o esta inactivo.
     */
    @PutMapping("/{sku}")
    public ResponseEntity<ProductoResponse> actualizar(
            @PathVariable String sku,
            @Valid @RequestBody ProductoRequest request) {
        return productoService.actualizar(sku, request)
                .map(ProductoResponse::desde)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/v1/catalogo/{sku}
     * Baja lógica por SKU: cambia {@code activo} a false (regla 1). Idempotente.
     * Responde 200 OK. Devuelve 404 si el SKU no existe.
     */
    @DeleteMapping("/{sku}")
    public ResponseEntity<Map<String, Object>> eliminar(@PathVariable String sku) {
        return productoService.eliminar(sku)
                .map(p -> ResponseEntity.ok(Map.<String, Object>of(
                        "sku", p.getSku(),
                        "mensaje", "Producto dado de baja correctamente"
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}