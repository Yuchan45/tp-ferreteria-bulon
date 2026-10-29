package com.bulon.stock.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    /** Stock de todos los productos en una sucursal, con su estado. */
    @GetMapping
    public List<StockSucursalDTO> porSucursal(@RequestParam Long sucursalId) {
        return stockService.listarPorSucursal(sucursalId);
    }
}
