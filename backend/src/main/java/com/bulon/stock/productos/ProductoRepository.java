package com.bulon.stock.productos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findByCodigo(String codigo);

    @Query("select p from Producto p where p.stockActual <= p.stockMinimo")
    List<Producto> findBajoStockMinimo();
}
