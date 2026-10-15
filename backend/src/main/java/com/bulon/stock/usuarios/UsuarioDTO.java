package com.bulon.stock.usuarios;

public record UsuarioDTO(Long id, String username, String nombre, Rol rol, boolean activo) {

    public static UsuarioDTO from(Usuario u) {
        return new UsuarioDTO(u.getId(), u.getUsername(), u.getNombre(), u.getRol(), u.isActivo());
    }
}
