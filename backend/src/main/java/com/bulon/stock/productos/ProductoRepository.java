package com.bulon.stock.productos;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    String STOCK_TOTAL = "(select coalesce(sum(e.cantidad), 0) from Existencia e where e.producto = p)";

    Optional<Producto> findByCodigo(String codigo);

    /** {@code texto} ya viene en minúsculas y con comodines ({@code %bulon%}) o null. */
    @Query("select p from Producto p where (:texto is null or lower(p.codigo) like :texto or lower(p.nombre) like :texto)"
            + " and (:categoriaId is null or p.categoria.id = :categoriaId)"
            + " and (:activo is null or p.activo = :activo)"
            + " and (:reposicion = false or (p.activo = true and " + STOCK_TOTAL + " <= p.puntoPedido))")
    Page<Producto> buscar(String texto, Long categoriaId, Boolean activo, boolean reposicion, Pageable pageable);

    @Query("select p from Producto p where p.activo = true and " + STOCK_TOTAL + " <= p.puntoPedido order by p.codigo")
    List<Producto> findPendientesReposicion();

    @Query("select count(p) from Producto p where p.activo = true and " + STOCK_TOTAL + " <= p.puntoPedido")
    long contarPendientesReposicion();

    @Query("select count(p) from Producto p where p.activo = true and " + STOCK_TOTAL + " <= p.stockMinimo")
    long contarBajoMinimo();

    long countByActivoTrue();
}
