package com.bulon.stock.movimientos;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {

    List<MovimientoStock> findByProductoIdOrderByFechaDescIdDesc(Long productoId);

    List<MovimientoStock> findByProductoIdAndSucursalIdOrderByFechaDescIdDesc(Long productoId, Long sucursalId);
}
