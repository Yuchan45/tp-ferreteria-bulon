package com.bulon.stock.usuarios;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos para crear un usuario. La contraseña llega en texto plano y se guarda con hash. */
public record UsuarioRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(max = 100) String nombre,
        @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Rol rol,
        /** Obligatoria para EMPLEADO; debe ser null para ADMINISTRADOR. */
        Long sucursalId,
        Boolean activo
) {
}
