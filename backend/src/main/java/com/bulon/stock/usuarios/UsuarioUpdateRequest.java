package com.bulon.stock.usuarios;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para editar un usuario. El username no cambia. La contraseña es opcional: si viene,
 * se reemplaza (el administrador la resetea); si no, queda la que tenía.
 */
public record UsuarioUpdateRequest(
        @NotBlank @Size(max = 100) String nombre,
        @Email @Size(max = 150) String email,
        @Size(min = 8, max = 72) String password,
        @NotNull Rol rol,
        /** Obligatoria para EMPLEADO; debe ser null para ADMINISTRADOR. */
        Long sucursalId,
        @NotNull Boolean activo
) {
}
