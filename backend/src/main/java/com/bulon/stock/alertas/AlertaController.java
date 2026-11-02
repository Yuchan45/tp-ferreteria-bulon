package com.bulon.stock.alertas;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/alertas")
@RequiredArgsConstructor
public class AlertaController {

    private final AlertaService alertaService;

    /** Alertas activas (sucursalId opcional): primero las críticas. */
    @GetMapping
    public List<AlertaDTO> activas(@RequestParam(required = false) Long sucursalId) {
        return alertaService.listarActivas(sucursalId);
    }
}
