package com.bulon.stock.alertas;

import com.bulon.stock.productos.EstadoStock;

import java.time.LocalDateTime;

public record AlertaDTO(Long id, Long productoId, String productoCodigo, String productoNombre,
                        EstadoStock estado, int stock, LocalDateTime fecha, LocalDateTime resueltaEn,
                        String mensaje) {

    public static AlertaDTO from(AlertaStock a) {
        return new AlertaDTO(a.getId(), a.getProducto().getId(), a.getProducto().getCodigo(),
                a.getProducto().getNombre(), a.getEstado(), a.getStock(), a.getFecha(), a.getResueltaEn(),
                a.mensaje());
    }
}
