package com.bulon.stock.movimientos;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/movimientos")
@RequiredArgsConstructor
public class MovimientoStockController {

    private final MovimientoStockService movimientoService;

    @GetMapping
    public List<MovimientoStockDTO> listarPorProducto(@RequestParam Long productoId) {
        return movimientoService.listarPorProducto(productoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoStockDTO registrar(@Valid @RequestBody MovimientoStockDTO dto) {
        return movimientoService.registrar(dto);
    }
}
