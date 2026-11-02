package com.bulon.stock.alertas;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AlertaStockRepository extends JpaRepository<AlertaStock, Long> {

    @Query("""
            select a from AlertaStock a
            where a.producto.id = :productoId and a.sucursal.id = :sucursalId
              and a.estado <> com.bulon.stock.alertas.EstadoAlerta.RESUELTA""")
    Optional<AlertaStock> findActiva(@Param("productoId") Long productoId, @Param("sucursalId") Long sucursalId);

    /** Activas, primero las críticas y luego las más antiguas (sucursalId null = todas). */
    @Query("""
            select a from AlertaStock a join fetch a.producto join fetch a.sucursal
            where a.estado <> com.bulon.stock.alertas.EstadoAlerta.RESUELTA
              and (:sucursalId is null or a.sucursal.id = :sucursalId)
            order by case when a.nivel = com.bulon.stock.alertas.NivelAlerta.CRITICO then 0 else 1 end, a.creada""")
    List<AlertaStock> findActivas(@Param("sucursalId") Long sucursalId);
}
