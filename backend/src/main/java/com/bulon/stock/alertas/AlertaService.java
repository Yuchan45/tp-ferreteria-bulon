package com.bulon.stock.alertas;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertaService {

    private final AlertaStockRepository alertaRepository;

    public Page<AlertaDTO> listar(boolean soloAbiertas, Pageable pageable) {
        return (soloAbiertas ? alertaRepository.findByResueltaEnIsNull(pageable) : alertaRepository.findAllBy(pageable))
                .map(AlertaDTO::from);
    }

    public long contarAbiertas() {
        return alertaRepository.countByResueltaEnIsNull();
    }
}
