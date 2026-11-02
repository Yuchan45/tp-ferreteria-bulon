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
        validarSucursal(request.rol(), request.sucursalId());

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

    /**
     * Edita un usuario: nombre, email, rol, sucursal, estado y, si se indica, la contraseña.
     * Para que el sistema no quede sin quien lo administre, nadie puede desactivarse ni
     * quitarse el rol a sí mismo, y siempre queda al menos un administrador activo.
     * Un usuario desactivado ya no puede iniciar sesión ni recibe notificaciones.
     */
    @Transactional
    public UsuarioDTO actualizar(Long id, UsuarioUpdateRequest request, Long editadoPor) {
        Usuario usuario = buscarEntidad(id);
        if (request.email() != null && usuarioRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new BusinessException("Ya existe un usuario con el email " + request.email());
        }
        validarSucursal(request.rol(), request.sucursalId());

        boolean dejaDeSerAdminActivo = usuario.getRol() == Rol.ADMINISTRADOR && usuario.isActivo()
                && (request.rol() != Rol.ADMINISTRADOR || !request.activo());
        if (dejaDeSerAdminActivo && id.equals(editadoPor)) {
            throw new BusinessException("No podés desactivarte ni quitarte el rol de administrador a vos mismo");
        }
        if (dejaDeSerAdminActivo && usuarioRepository.countByRolAndActivoTrue(Rol.ADMINISTRADOR) <= 1) {
            throw new BusinessException("Tiene que quedar al menos un administrador activo");
        }

        usuario.setNombre(request.nombre().trim());
        usuario.setEmail(request.email() == null || request.email().isBlank() ? null : request.email().trim());
        usuario.setRol(request.rol());
        usuario.setSucursal(request.sucursalId() != null ? sucursalService.obtener(request.sucursalId()) : null);
        usuario.setActivo(request.activo());
        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return UsuarioDTO.from(usuario);
    }

    /** Misma regla que el CHECK ck_usuarios_sucursal de la base. */
    private static void validarSucursal(Rol rol, Long sucursalId) {
        if (rol == Rol.EMPLEADO && sucursalId == null) {
            throw new BusinessException("Un empleado debe pertenecer a una sucursal");
        }
        if (rol == Rol.ADMINISTRADOR && sucursalId != null) {
            throw new BusinessException("Un administrador no se asigna a una sucursal: ve todas");
        }
    }

    /** Uso interno del login. */
    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    /** Uso interno de las notificaciones: quiénes deben enterarse de lo que pasa en una sucursal. */
    public List<Usuario> activosDeSucursal(Long sucursalId) {
        return usuarioRepository.findActivosDeSucursal(sucursalId);
    }

    /** Uso interno de otros módulos (p. ej. el responsable de un movimiento). */
    public Usuario buscarEntidad(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }
}
