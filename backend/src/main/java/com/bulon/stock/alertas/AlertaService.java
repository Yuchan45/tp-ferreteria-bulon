package com.bulon.stock.alertas;

import com.bulon.stock.stock.EstadoStock;
import com.bulon.stock.stock.StockObserver;
import com.bulon.stock.stock.StockSucursal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Observa el stock (es un {@link StockObserver}) y mantiene las alertas al día:
 * la abre al llegar al punto de pedido, la sube a CRITICO al llegar al mínimo y
 * la resuelve cuando el stock se recupera. A su vez es Subject de los {@link AlertaObserver}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertaService implements StockObserver {

    private final AlertaStockRepository alertaRepository;
    private final List<AlertaObserver> observadores;

    public List<AlertaDTO> listarActivas(Long sucursalId) {
        return alertaRepository.findActivas(sucursalId).stream().map(AlertaDTO::from).toList();
    }

    @Override
    @Transactional
    public void stockActualizado(StockSucursal stock) {
        Optional<AlertaStock> activa = alertaRepository.findActiva(stock.getProducto().getId(), stock.getSucursal().getId());
        EstadoStock estado = stock.estado();

        if (estado == EstadoStock.OK) {
            activa.ifPresent(alerta -> {
                alerta.resolver();
                avisar(alerta, CambioAlerta.RESUELTA);
            });
            return;
        }

        NivelAlerta nivel = estado == EstadoStock.CRITICO ? NivelAlerta.CRITICO : NivelAlerta.REPONER;
        if (activa.isEmpty()) {
            avisar(alertaRepository.save(new AlertaStock(stock, nivel)), CambioAlerta.ABIERTA);
        } else if (activa.get().getNivel() != nivel) {
            AlertaStock alerta = activa.get();
            alerta.actualizar(stock, nivel);
            // Volver de CRITICO a REPONER no es novedad para nadie; subir a CRITICO, sí
            if (nivel == NivelAlerta.CRITICO) {
                avisar(alerta, CambioAlerta.ESCALADA);
            }
        }
    }

    private void avisar(AlertaStock alerta, CambioAlerta cambio) {
        observadores.forEach(observador -> observador.alertaActualizada(alerta, cambio));
    }
}
