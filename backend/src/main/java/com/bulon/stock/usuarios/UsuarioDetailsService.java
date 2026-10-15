package com.bulon.stock.usuarios;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        return User.withUsername(usuario.getUsername())
                .password(usuario.getPasswordHash() != null ? usuario.getPasswordHash() : "")
                .roles(usuario.getRol().name())
                .disabled(!usuario.isActivo() || usuario.getPasswordHash() == null)
                .build();
    }
}
