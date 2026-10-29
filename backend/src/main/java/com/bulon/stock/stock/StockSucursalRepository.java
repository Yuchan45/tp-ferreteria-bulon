package com.bulon.stock.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StockSucursalRepository extends JpaRepository<StockSucursal, StockSucursalId> {

    @Query("""
            select s from StockSucursal s join fetch s.producto join fetch s.sucursal
            where s.id.productoId = :productoId order by s.sucursal.nombre""")
    List<StockSucursal> findByProductoId(Long productoId);

    @Query("""
            select s from StockSucursal s join fetch s.producto join fetch s.sucursal
            where s.id.sucursalId = :sucursalId order by s.producto.codigo""")
    List<StockSucursal> findBySucursalId(Long sucursalId);

    /** En o por debajo del punto de pedido; sucursalId null = todas las sucursales. */
    @Query("""
            select s from StockSucursal s join fetch s.producto join fetch s.sucursal
            where s.cantidad <= s.puntoPedido and (:sucursalId is null or s.id.sucursalId = :sucursalId)
            order by s.sucursal.nombre, s.producto.codigo""")
    List<StockSucursal> findAReponer(Long sucursalId);

    /** Stock total de cada producto sumando todas las sucursales: [productoId, total]. */
    @Query("select s.id.productoId, sum(s.cantidad) from StockSucursal s group by s.id.productoId")
    List<Object[]> sumarPorProducto();
}
