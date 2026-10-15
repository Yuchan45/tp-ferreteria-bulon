package com.bulon.stock.compras;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {

    @EntityGraph(attributePaths = {"proveedor", "usuario"})
    Page<OrdenCompra> findByEstado(EstadoOrdenCompra estado, Pageable pageable);

    @EntityGraph(attributePaths = {"proveedor", "usuario"})
    Page<OrdenCompra> findAllBy(Pageable pageable);
}
