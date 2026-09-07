package com.calisat.mscatalogo.repository;

import com.calisat.mscatalogo.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findBySku(String sku);

    boolean existsBySku(String sku);

    Page<Producto> findByActivoTrue(Pageable pageable);

    List<Producto> findByCategoriaAndActivoTrue(String categoria);
}
