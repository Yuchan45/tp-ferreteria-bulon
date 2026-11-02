package com.bulon.stock.productos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductoProveedorRepository extends JpaRepository<ProductoProveedor, ProductoProveedorId> {

    @Query("select pp from ProductoProveedor pp join fetch pp.proveedor where pp.id.productoId = :productoId")
    List<ProductoProveedor> findByProductoId(Long productoId);

    List<ProductoProveedor> findByIdProductoIdAndPreferidoTrue(Long productoId);

    boolean existsByIdProductoIdAndIdProveedorId(Long productoId, Long proveedorId);
}
