package com.bulon.stock.seguridad;

import com.bulon.stock.usuarios.UsuarioDTO;
import com.bulon.stock.usuarios.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/** El login del frontend es un GET /auth/me con las credenciales: 200 = válidas, 401 = no. */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;

    @GetMapping("/me")
    public UsuarioDTO me(Principal principal) {
        return UsuarioDTO.from(usuarioService.buscarPorUsername(principal.getName()));
    }
}
