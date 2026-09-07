package com.califorge.mscatalogo.controller;

import com.califorge.mscatalogo.dto.ProductoRequest;
import com.califorge.mscatalogo.dto.ProductoResponse;
import com.califorge.mscatalogo.model.Producto;
import com.califorge.mscatalogo.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * El identificador canónico del recurso es el {@code sku} (unificado en Fase 2).
 */
@RestController
@RequestMapping("/api/v1/catalogo")
@Tag(name = "Catalogo de productos", description = "Lectura publica del catalogo; POST/PUT/DELETE exigen JWT. El identificador canonico del recurso es el SKU.")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    /**
     * GET /api/v1/catalogo
     * Lista productos activos con paginacion.
     */
    @Operation(summary = "Listar productos activos", description = "Devuelve productos activos paginados (acceso publico).")
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
    @Operation(summary = "Consultar producto por SKU", description = "Detalle de un producto activo identificado por su SKU (identificador canonico). 404 si no existe o esta inactivo.")
    @GetMapping("/{sku}")
    public ResponseEntity<ProductoResponse> buscarPorSku(
            @Parameter(name = "sku", description = "SKU del producto (identificador canonico), p.ej. ANILLAS-001.", required = true)
            @PathVariable String sku) {
        return productoService.buscarPorSku(sku)
                .map(ProductoResponse::desde)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/catalogo/categoria/{categoria}
     * Filtra productos activos por categoría.
     */
    @Operation(summary = "Listar productos por categoria", description = "Productos activos filtrados por categoria (acceso publico).")
    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<ProductoResponse>> listarPorCategoria(
            @Parameter(name = "categoria", description = "Categoria a filtrar.", required = true)
            @PathVariable String categoria) {
        List<ProductoResponse> productos = productoService.listarPorCategoria(categoria).stream()
                .map(ProductoResponse::desde)
                .toList();
        return ResponseEntity.ok(productos);
    }

    /**
     * POST /api/v1/catalogo
     * Crea un producto. Devuelve 400 si el SKU ya existe, 201 en exito con Location.
     */
    @Operation(summary = "Crear producto", description = "Crea un producto con SKU unico. 201 con Location por SKU; 400 si el SKU ya existe o la entrada es invalida. Requiere JWT.")
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
    @Operation(summary = "Actualizar producto por SKU", description = "Reemplaza el estado completo del producto identificado por su SKU (identificador canonico). 404 si no existe o esta inactivo. Requiere JWT.")
    @PutMapping("/{sku}")
    public ResponseEntity<ProductoResponse> actualizar(
            @Parameter(name = "sku", description = "SKU del producto a actualizar (identificador canonico).", required = true)
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
    @Operation(summary = "Dar de baja producto por SKU", description = "Baja logica por SKU (identificador canonico): activo pasa a false, el registro se conserva. Idempotente: 200 incluso si ya estaba inactivo; 404 si el SKU no existe. Requiere JWT.")
    @DeleteMapping("/{sku}")
    public ResponseEntity<Map<String, Object>> eliminar(
            @Parameter(name = "sku", description = "SKU del producto a dar de baja (identificador canonico).", required = true)
            @PathVariable String sku) {
        return productoService.eliminar(sku)
                .map(p -> ResponseEntity.ok(Map.<String, Object>of(
                        "sku", p.getSku(),
                        "mensaje", "Producto dado de baja correctamente"
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}