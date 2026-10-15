package com.bulon.stock.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExistenciaRepository extends JpaRepository<Existencia, Long> {

    Optional<Existencia> findByProductoIdAndUbicacionId(Long productoId, Long ubicacionId);

    List<Existencia> findByProductoIdIn(Collection<Long> productoIds);

    @Query("""
            select new com.bulon.stock.stock.UnidadesPorUbicacion(u.id, u.nombre, coalesce(sum(e.cantidad), 0))
            from Ubicacion u left join Existencia e on e.ubicacion = u
            group by u.id, u.nombre order by u.id""")
    List<UnidadesPorUbicacion> unidadesPorUbicacion();
}
