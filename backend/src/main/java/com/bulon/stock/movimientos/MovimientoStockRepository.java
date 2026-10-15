package com.bulon.stock.movimientos;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {

    @EntityGraph(attributePaths = {"producto", "ubicacion", "ubicacionDestino", "usuario"})
    @Query("""
            select m from MovimientoStock m left join m.ubicacionDestino d
            where (:productoId is null or m.producto.id = :productoId)
              and (:tipo is null or m.tipo = :tipo)
              and (:usuarioId is null or m.usuario.id = :usuarioId)
              and (:ubicacionId is null or m.ubicacion.id = :ubicacionId or d.id = :ubicacionId)
              and (:desde is null or m.fecha >= :desde)
              and (:hasta is null or m.fecha < :hasta)""")
    Page<MovimientoStock> buscar(Long productoId, TipoMovimiento tipo, Long usuarioId, Long ubicacionId,
                                 LocalDateTime desde, LocalDateTime hasta, Pageable pageable);

    long countByFechaGreaterThanEqual(LocalDateTime desde);

    /** Unidades vendidas por producto en el período, de mayor a menor: [productoId, unidades]. */
    @Query("""
            select m.producto.id, sum(m.cantidad) from MovimientoStock m
            where m.tipo = com.bulon.stock.movimientos.TipoMovimiento.SALIDA_VENTA
              and m.fecha >= :desde and m.fecha < :hasta
            group by m.producto.id order by sum(m.cantidad) desc""")
    List<Object[]> unidadesVendidas(LocalDateTime desde, LocalDateTime hasta, Pageable pageable);
}
