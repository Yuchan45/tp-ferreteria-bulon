package com.bulon.stock.auth;

import com.bulon.stock.auth.AuthDTOs.LoginRequest;
import com.bulon.stock.auth.AuthDTOs.RegistroRequest;
import com.bulon.stock.auth.AuthDTOs.SesionResponse;
import com.bulon.stock.usuarios.UsuarioDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Público. Devuelve el token que el cliente envía después en Authorization: Bearer. */
    @PostMapping("/login")
    public SesionResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Público. Crea un EMPLEADO y devuelve su sesión (acceso inmediato). */
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public SesionResponse registrar(@Valid @RequestBody RegistroRequest request) {
        return authService.registrar(request);
    }

    /** Requiere token. Datos del usuario logueado. */
    @GetMapping("/yo")
    public UsuarioDTO yo(@AuthenticationPrincipal Jwt jwt) {
        return authService.yo(TokenService.usuarioId(jwt));
    }
}
