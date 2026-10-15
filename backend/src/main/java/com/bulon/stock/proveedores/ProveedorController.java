package com.bulon.stock.proveedores;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService proveedorService;

    @GetMapping
    public List<ProveedorDTO> listar() {
        return proveedorService.listar();
    }

    @GetMapping("/{id}")
    public ProveedorDTO obtener(@PathVariable Long id) {
        return proveedorService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProveedorDTO crear(@Valid @RequestBody ProveedorRequest request) {
        return proveedorService.crear(request);
    }

    @PutMapping("/{id}")
    public ProveedorDTO actualizar(@PathVariable Long id, @Valid @RequestBody ProveedorRequest request) {
        return proveedorService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        proveedorService.eliminar(id);
    }
}
