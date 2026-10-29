package com.bulon.stock.usuarios;

import com.bulon.stock.common.exception.BusinessException;
import com.bulon.stock.common.exception.ResourceNotFoundException;
import com.bulon.stock.sucursales.SucursalService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final SucursalService sucursalService;
    private final PasswordEncoder passwordEncoder;

    public List<UsuarioDTO> listar() {
        return usuarioRepository.findAll().stream().map(UsuarioDTO::from).toList();
    }

    public UsuarioDTO obtener(Long id) {
        return UsuarioDTO.from(buscarEntidad(id));
    }

    @Transactional
    public UsuarioDTO crear(UsuarioRequest request) {
        if (usuarioRepository.existsByUsername(request.username())) {
            throw new BusinessException("Ya existe el usuario " + request.username());
        }
        if (request.email() != null && usuarioRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException("Ya existe un usuario con el email " + request.email());
        }
        // Misma regla que el CHECK ck_usuarios_sucursal de la base
        if (request.rol() == Rol.EMPLEADO && request.sucursalId() == null) {
            throw new BusinessException("Un empleado debe pertenecer a una sucursal");
        }
        if (request.rol() == Rol.ADMINISTRADOR && request.sucursalId() != null) {
            throw new BusinessException("Un administrador no se asigna a una sucursal: ve todas");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(request.username());
        usuario.setNombre(request.nombre());
        usuario.setEmail(request.email());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(request.rol());
        usuario.setSucursal(request.sucursalId() != null ? sucursalService.obtener(request.sucursalId()) : null);
        usuario.setActivo(request.activo() == null || request.activo());
        return UsuarioDTO.from(usuarioRepository.save(usuario));
    }

    /** Uso interno del login. */
    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    /** Uso interno de otros módulos (p. ej. el responsable de un movimiento). */
    public Usuario buscarEntidad(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }
}
