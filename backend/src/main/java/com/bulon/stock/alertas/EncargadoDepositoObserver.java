package com.bulon.stock.alertas;

import com.bulon.stock.movimientos.StockObserver;
import com.bulon.stock.productos.EstadoStock;
import com.bulon.stock.productos.Producto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Observer concreto: avisa al encargado de depósito cuando un producto pasa a estar pendiente de
 * reposición o empeora (punto de pedido → bajo mínimo), y cierra la alerta cuando se repone.
 * Repetir la notificación con el mismo estado no genera otra alerta.
 */
@Component
@RequiredArgsConstructor
public class EncargadoDepositoObserver implements StockObserver {

    private final AlertaStockRepository alertaRepository;
    private final Alerta alerta;

    @Override
    public void stockActualizado(Producto producto, int stockTotal) {
        EstadoStock estado = producto.estadoStock(stockTotal);
        Optional<AlertaStock> abierta = alertaRepository.findByProductoIdAndResueltaEnIsNull(producto.getId());

        if (!estado.pendienteReposicion()) {
            abierta.ifPresent(a -> a.setResueltaEn(LocalDateTime.now()));
            return;
        }
        if (abierta.isPresent() && abierta.get().getEstado() == estado) {
            abierta.get().setStock(stockTotal);
            return;
        }
        AlertaStock nueva = abierta.orElseGet(AlertaStock::new);
        nueva.setProducto(producto);
        nueva.setEstado(estado);
        nueva.setStock(stockTotal);
        nueva.setFecha(LocalDateTime.now());
        alerta.enviar(nueva);
    }
}
