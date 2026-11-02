package com.bulon.stock.movimientos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {

    /**
     * Historial completo de un producto, de una sucursal o de ambos (los null no filtran),
     * del más reciente al más antiguo, con producto, responsable y proveedor ya cargados.
     */
    @Query("""
            select m from MovimientoStock m
              join fetch m.producto join fetch m.usuario left join fetch m.proveedor
            where (:productoId is null or m.producto.id = :productoId)
              and (:sucursalId is null or m.sucursal.id = :sucursalId)
            order by m.fecha desc, m.id desc""")
    List<MovimientoStock> findHistorial(@Param("productoId") Long productoId, @Param("sucursalId") Long sucursalId);
}
