package com.bulon.stock.alertas;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlertaStockRepository extends JpaRepository<AlertaStock, Long> {

    Optional<AlertaStock> findByProductoIdAndResueltaEnIsNull(Long productoId);

    @EntityGraph(attributePaths = "producto")
    Page<AlertaStock> findByResueltaEnIsNull(Pageable pageable);

    @EntityGraph(attributePaths = "producto")
    Page<AlertaStock> findAllBy(Pageable pageable);

    long countByResueltaEnIsNull();
}
