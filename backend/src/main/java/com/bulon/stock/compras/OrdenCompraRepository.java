package com.bulon.stock.compras;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {

    @EntityGraph(attributePaths = {"proveedor", "usuario"})
    Page<OrdenCompra> findByEstado(EstadoOrdenCompra estado, Pageable pageable);

    @EntityGraph(attributePaths = {"proveedor", "usuario"})
    Page<OrdenCompra> findAllBy(Pageable pageable);

    /** Bloquea la fila: dos "recibir" simultáneos no pueden ingresar el stock dos veces. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrdenCompra o where o.id = :id")
    Optional<OrdenCompra> findParaCambiarEstado(Long id);
}
