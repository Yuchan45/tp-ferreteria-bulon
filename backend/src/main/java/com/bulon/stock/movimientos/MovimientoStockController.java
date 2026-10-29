package com.bulon.stock.movimientos;

import com.bulon.stock.auth.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/movimientos")
@RequiredArgsConstructor
public class MovimientoStockController {

    private final MovimientoStockService movimientoService;

    /** Historial de un producto; con sucursalId, solo el de esa sucursal. */
    @GetMapping
    public List<MovimientoStockDTO> listarPorProducto(@RequestParam Long productoId,
                                                      @RequestParam(required = false) Long sucursalId) {
        return movimientoService.listarPorProducto(productoId, sucursalId);
    }

    /**
     * Cualquier usuario logueado puede registrar movimientos. El responsable es siempre el
     * usuario del token (se ignora un usuarioId enviado en el body), y el servicio controla
     * que un EMPLEADO solo opere su sucursal.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoStockDTO registrar(@Valid @RequestBody MovimientoStockDTO dto, @AuthenticationPrincipal Jwt jwt) {
        return movimientoService.registrar(dto.conUsuario(TokenService.usuarioId(jwt)));
    }
}
