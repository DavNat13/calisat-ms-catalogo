package com.califorge.mscatalogo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "sku es obligatorio")
    @Size(max = 64, message = "sku no puede superar 64 caracteres")
    @Column(name = "sku", unique = true, nullable = false, length = 64)
    private String sku;

    @NotBlank(message = "nombre es obligatorio")
    @Size(max = 120, message = "nombre no puede superar 120 caracteres")
    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Size(max = 1000, message = "descripcion no puede superar 1000 caracteres")
    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    @NotNull(message = "precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "precio debe ser mayor a 0")
    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @NotBlank(message = "categoria es obligatoria")
    @Size(max = 64, message = "categoria no puede superar 64 caracteres")
    @Column(name = "categoria", nullable = false, length = 64)
    private String categoria;

    @Size(max = 500, message = "imagenUrl no puede superar 500 caracteres")
    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
    }

    public Producto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}