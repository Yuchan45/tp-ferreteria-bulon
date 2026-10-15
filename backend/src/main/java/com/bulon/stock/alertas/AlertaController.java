package com.bulon.stock.alertas;

import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/alertas")
@RequiredArgsConstructor
public class AlertaController {

    private final AlertaService alertaService;

    @GetMapping
    public Page<AlertaDTO> listar(@RequestParam(defaultValue = "true") boolean abiertas,
                                  @ParameterObject @PageableDefault(size = 20, sort = "fecha",
                                          direction = Sort.Direction.DESC) Pageable pageable) {
        return alertaService.listar(abiertas, pageable);
    }
}
