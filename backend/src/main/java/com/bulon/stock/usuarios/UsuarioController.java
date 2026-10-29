package com.bulon.stock.usuarios;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Gestión de usuarios: solo el administrador (la anotación en la clase aplica a todos los métodos). */
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public List<UsuarioDTO> listar() {
        return usuarioService.listar();
    }

    @GetMapping("/{id}")
    public UsuarioDTO obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDTO crear(@Valid @RequestBody UsuarioRequest request) {
        return usuarioService.crear(request);
    }
}
