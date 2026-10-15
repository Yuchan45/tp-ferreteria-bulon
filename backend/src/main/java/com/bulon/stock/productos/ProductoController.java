package com.bulon.stock.productos;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    /** {@code reposicion=true} lista solo los pendientes de reposición (stock total ≤ punto de pedido). */
    @GetMapping
    public Page<ProductoDTO> listar(@RequestParam(required = false) String q,
                                    @RequestParam(required = false) Long categoriaId,
                                    @RequestParam(required = false) Boolean activo,
                                    @RequestParam(defaultValue = "false") boolean reposicion,
                                    @ParameterObject @PageableDefault(size = 20, sort = "codigo") Pageable pageable) {
        return productoService.listar(q, categoriaId, activo, reposicion, pageable);
    }

    @GetMapping("/{id}")
    public ProductoDTO obtener(@PathVariable Long id) {
        return productoService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoDTO crear(@Valid @RequestBody ProductoRequest request) {
        return productoService.crear(request);
    }

    @PutMapping("/{id}")
    public ProductoDTO actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return productoService.actualizar(id, request);
    }
}
