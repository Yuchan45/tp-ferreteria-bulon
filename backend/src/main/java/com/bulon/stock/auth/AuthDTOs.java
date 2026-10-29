package com.bulon.stock.auth;

import com.bulon.stock.usuarios.UsuarioDTO;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** Datos que entran y salen de /auth. */
public final class AuthDTOs {

    private AuthDTOs() {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    /** Registro público: siempre crea un EMPLEADO; el rol no se elige desde afuera. */
    public record RegistroRequest(
            @NotBlank @Size(max = 50) String username,
            @NotBlank @Size(max = 100) String nombre,
            @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotNull Long sucursalId
    ) {
    }

    /** Respuesta de login y registro: el token y los datos del usuario. */
    public record SesionResponse(String token, Instant expira, UsuarioDTO usuario) {
    }
}
