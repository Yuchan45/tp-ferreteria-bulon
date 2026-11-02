package com.bulon.stock.movimientos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {

    List<MovimientoStock> findByProductoIdOrderByFechaDescIdDesc(Long productoId);

    List<MovimientoStock> findByProductoIdAndSucursalIdOrderByFechaDescIdDesc(Long productoId, Long sucursalId);

    /** Movimientos de una sucursal en [desde, hasta), con tipo opcional; los null no filtran. */
    @Query("""
            select m from MovimientoStock m
              join fetch m.producto join fetch m.usuario left join fetch m.proveedor
            where m.sucursal.id = :sucursalId
              and (:desde is null or m.fecha >= :desde)
              and (:hasta is null or m.fecha < :hasta)
              and (:tipo is null or m.tipo = :tipo)
            order by m.fecha desc, m.id desc""")
    List<MovimientoStock> findDeSucursal(@Param("sucursalId") Long sucursalId,
                                         @Param("desde") LocalDateTime desde,
                                         @Param("hasta") LocalDateTime hasta,
                                         @Param("tipo") TipoMovimiento tipo);
}
