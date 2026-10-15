package com.bulon.stock.stock;

import com.bulon.stock.productos.Producto;
import com.bulon.stock.ubicaciones.Ubicacion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Stock de un producto en una ubicación. Solo cambia a través de un movimiento de stock. */
@Entity
@Table(name = "existencias")
@Getter
@NoArgsConstructor
public class Existencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ubicacion_id", nullable = false)
    private Ubicacion ubicacion;

    @Column(nullable = false)
    private int cantidad;

    /** Bloqueo optimista: si dos movimientos leen la misma existencia, el segundo en guardar falla. */
    @Version
    private Long version;

    Existencia(Producto producto, Ubicacion ubicacion) {
        this.producto = producto;
        this.ubicacion = ubicacion;
    }

    void sumar(int delta) {
        cantidad += delta;
    }
}
