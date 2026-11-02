package com.bulon.stock.usuarios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Usuarios activos que operan la sucursal: sus empleados y todos los administradores. */
    @Query("""
            select u from Usuario u
            where u.activo = true
              and (u.rol = com.bulon.stock.usuarios.Rol.ADMINISTRADOR or u.sucursal.id = :sucursalId)""")
    List<Usuario> findActivosDeSucursal(@Param("sucursalId") Long sucursalId);

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);

    /** Otro usuario (distinto de {@code id}) ya usa ese email. */
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    long countByRolAndActivoTrue(Rol rol);
}
