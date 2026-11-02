package com.bulon.stock.movimientos;

import com.bulon.stock.auth.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/movimientos")
@RequiredArgsConstructor
public class MovimientoStockController {

    private final MovimientoStockService movimientoService;

    /**
     * Historial de un producto, de una sucursal o de un producto en una sucursal, con el stock
     * resultante de cada movimiento. Filtros opcionales: período (yyyy-mm-dd, ambos días
     * incluidos), tipo y búsqueda por código o nombre del producto.
     */
    @GetMapping
    public List<MovimientoDetalleDTO> listar(@RequestParam(required = false) Long productoId,
                                             @RequestParam(required = false) Long sucursalId,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                             @RequestParam(required = false) TipoMovimiento tipo,
                                             @RequestParam(required = false) String q) {
        return movimientoService.listar(new FiltroMovimientos(productoId, sucursalId, desde, hasta, tipo, q));
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
