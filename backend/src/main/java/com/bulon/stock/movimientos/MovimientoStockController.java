package com.bulon.stock.movimientos;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/** Sin PUT ni DELETE: los movimientos son inmutables. */
@RestController
@RequestMapping("/movimientos")
@RequiredArgsConstructor
public class MovimientoStockController {

    private final MovimientoStockService movimientoService;

    @GetMapping
    public Page<MovimientoDTO> listar(@ParameterObject FiltroMovimientos filtro,
                                      @ParameterObject @PageableDefault(size = 20, sort = "fecha",
                                              direction = Sort.Direction.DESC) Pageable pageable) {
        return movimientoService.listar(filtro, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoDTO registrar(@Valid @RequestBody MovimientoRequest request, Principal principal) {
        return movimientoService.registrar(request, principal.getName());
    }
}
