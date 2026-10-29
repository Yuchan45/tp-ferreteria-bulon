package com.bulon.stock.usuarios;

import java.time.LocalDateTime;

/** Lo que la API muestra de un usuario: nunca incluye el hash de la contraseña. */
public record UsuarioDTO(
        Long id,
        String username,
        String nombre,
        String email,
        Rol rol,
        Long sucursalId,
        boolean activo,
        LocalDateTime creado
) {

    public static UsuarioDTO from(Usuario u) {
        return new UsuarioDTO(
                u.getId(),
                u.getUsername(),
                u.getNombre(),
                u.getEmail(),
                u.getRol(),
                u.getSucursal() != null ? u.getSucursal().getId() : null,
                u.isActivo(),
                u.getCreado()
        );
    }
}
