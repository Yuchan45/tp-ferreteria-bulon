package com.bulon.stock.movimientos;

import com.bulon.stock.productos.Producto;

/**
 * Observer (Entrega 2, 3.4). {@link MovimientoStockService} es el Subject: después de cada
 * movimiento notifica el nuevo stock total del producto ({@code ProductoService} también, al cambiar
 * los umbrales). Spring suscribe automáticamente todo bean que implemente esta interfaz.
 */
public interface StockObserver {

    void stockActualizado(Producto producto, int stockTotal);
}
