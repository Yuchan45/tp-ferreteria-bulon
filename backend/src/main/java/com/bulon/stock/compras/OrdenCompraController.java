package com.bulon.stock.compras;

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
import java.util.List;

@RestController
@RequestMapping("/ordenes-compra")
@RequiredArgsConstructor
public class OrdenCompraController {

    private final OrdenCompraService ordenService;

    @GetMapping
    public Page<OrdenCompraDTO> listar(@RequestParam(required = false) EstadoOrdenCompra estado,
                                       @ParameterObject @PageableDefault(size = 20, sort = "fechaCreacion",
                                               direction = Sort.Direction.DESC) Pageable pageable) {
        return ordenService.listar(estado, pageable);
    }

    @GetMapping("/sugerencias")
    public List<SugerenciaCompraDTO> sugerencias() {
        return ordenService.sugerencias();
    }

    @GetMapping("/{id}")
    public OrdenCompraDTO obtener(@PathVariable Long id) {
        return ordenService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrdenCompraDTO crear(@Valid @RequestBody OrdenCompraRequest request, Principal principal) {
        return ordenService.crear(request, principal.getName());
    }

    @PostMapping("/{id}/enviar")
    public OrdenCompraDTO enviar(@PathVariable Long id) {
        return ordenService.enviar(id);
    }

    @PostMapping("/{id}/recibir")
    public OrdenCompraDTO recibir(@PathVariable Long id, @Valid @RequestBody RecepcionRequest request,
                                  Principal principal) {
        return ordenService.recibir(id, request, principal.getName());
    }

    @PostMapping("/{id}/cancelar")
    public OrdenCompraDTO cancelar(@PathVariable Long id) {
        return ordenService.cancelar(id);
    }
}
