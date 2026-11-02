package com.bulon.stock.sucursales;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sucursales")
@RequiredArgsConstructor
public class SucursalController {

    private final SucursalService sucursalService;

    @GetMapping
    public List<Sucursal> listar() {
        return sucursalService.listar();
    }

    @GetMapping("/{id}")
    public Sucursal obtener(@PathVariable Long id) {
        return sucursalService.obtener(id);
    }
}
