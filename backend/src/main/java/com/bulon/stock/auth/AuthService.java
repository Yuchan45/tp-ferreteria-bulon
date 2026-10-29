package com.bulon.stock.auth;

import com.bulon.stock.auth.AuthDTOs.LoginRequest;
import com.bulon.stock.auth.AuthDTOs.RegistroRequest;
import com.bulon.stock.auth.AuthDTOs.SesionResponse;
import com.bulon.stock.usuarios.Rol;
import com.bulon.stock.usuarios.Usuario;
import com.bulon.stock.usuarios.UsuarioDTO;
import com.bulon.stock.usuarios.UsuarioRequest;
import com.bulon.stock.usuarios.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public SesionResponse login(LoginRequest request) {
        Usuario usuario = usuarioService.buscarPorUsername(request.username())
                // El mismo mensaje para usuario inexistente y contraseña incorrecta:
                // así no se puede averiguar qué usuarios existen
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new CredencialesInvalidasException("Usuario o contraseña incorrectos"));
        if (!usuario.isActivo()) {
            throw new CredencialesInvalidasException("El usuario está inactivo. Consultá con un administrador.");
        }
        return sesion(usuario);
    }

    /** Registro público con acceso inmediato: crea un EMPLEADO de la sucursal elegida y lo loguea. */
    @Transactional
    public SesionResponse registrar(RegistroRequest request) {
        UsuarioDTO creado = usuarioService.crear(new UsuarioRequest(request.username(), request.nombre(),
                request.email(), request.password(), Rol.EMPLEADO, request.sucursalId(), true));
        return sesion(usuarioService.buscarEntidad(creado.id()));
    }

    public UsuarioDTO yo(Long usuarioId) {
        return usuarioService.obtener(usuarioId);
    }

    private SesionResponse sesion(Usuario usuario) {
        TokenService.Token token = tokenService.generar(usuario);
        return new SesionResponse(token.valor(), token.expira(), UsuarioDTO.from(usuario));
    }
}
