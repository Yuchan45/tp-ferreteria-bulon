package com.bulon.stock.usuarios;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code password} es obligatoria al crear; al actualizar, null conserva la actual. */
public record UsuarioRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(max = 100) String nombre,
        @NotNull Rol rol,
        Boolean activo,
        @Size(min = 6, max = 72) String password
) {
}
