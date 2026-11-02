package com.bulon.stock.notificaciones;

import com.bulon.stock.auth.TokenService;
import com.bulon.stock.notificaciones.NotificacionService.BandejaDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** Cada usuario ve y marca solo sus notificaciones: el usuario sale siempre del token. */
@RestController
@RequestMapping("/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping
    public BandejaDTO bandeja(@AuthenticationPrincipal Jwt jwt) {
        return notificacionService.bandeja(TokenService.usuarioId(jwt));
    }

    @PutMapping("/{id}/leida")
    public NotificacionDTO marcarLeida(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return notificacionService.marcarLeida(id, TokenService.usuarioId(jwt));
    }

    @PutMapping("/leidas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarTodasLeidas(@AuthenticationPrincipal Jwt jwt) {
        notificacionService.marcarTodasLeidas(TokenService.usuarioId(jwt));
    }
}
