package com.bulon.stock.stock;

/**
 * Patrón Observer: quien necesite enterarse de los cambios de stock implementa esta
 * interfaz y se registra como bean ({@code @Service}/{@code @Component}). {@link StockService}
 * (el Subject) recibe la lista de observadores y les avisa sin conocer a ninguno en particular.
 */
public interface StockObserver {

    /** Se llama cada vez que cambia la cantidad o los umbrales de un stock. */
    void stockActualizado(StockSucursal stock);
}
