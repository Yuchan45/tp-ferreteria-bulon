package com.bulon.stock.usuarios;

import com.bulon.stock.common.exception.BusinessException;
import com.bulon.stock.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UsuarioDTO> listar() {
        return usuarioRepository.findAll(Sort.by("username")).stream().map(UsuarioDTO::from).toList();
    }

    public UsuarioDTO obtener(Long id) {
        return UsuarioDTO.from(buscarEntidad(id));
    }

    /** Usuario autenticado: el responsable de cada movimiento sale de acá, nunca del body. */
    public Usuario buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", username));
    }

    @Transactional
    public UsuarioDTO crear(UsuarioRequest request) {
        if (request.password() == null) {
            throw new BusinessException("La contraseña es obligatoria al crear un usuario");
        }
        Usuario usuario = new Usuario();
        aplicar(request, usuario);
        return UsuarioDTO.from(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioDTO actualizar(Long id, UsuarioRequest request) {
        Usuario usuario = buscarEntidad(id);
        aplicar(request, usuario);
        return UsuarioDTO.from(usuario);
    }

    private Usuario buscarEntidad(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }

    private void aplicar(UsuarioRequest request, Usuario usuario) {
        usuarioRepository.findByUsername(request.username())
                .filter(u -> !u.getId().equals(usuario.getId()))
                .ifPresent(u -> {
                    throw new BusinessException("Ya existe el usuario " + request.username());
                });
        usuario.setUsername(request.username().trim());
        usuario.setNombre(request.nombre().trim());
        usuario.setRol(request.rol());
        usuario.setActivo(request.activo() == null || request.activo());
        if (request.password() != null) {
            usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        }
    }
}
