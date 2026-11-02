package com.bulon.stock.stock;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** Clave compuesta de stock_sucursal: (producto, sucursal). */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StockSucursalId implements Serializable {

    @Column(name = "producto_id")
    private Long productoId;

    @Column(name = "sucursal_id")
    private Long sucursalId;
}
