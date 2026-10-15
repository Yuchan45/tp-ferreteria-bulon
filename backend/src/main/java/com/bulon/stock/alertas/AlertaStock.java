package com.bulon.stock.alertas;

import com.bulon.stock.productos.EstadoStock;
import com.bulon.stock.productos.Producto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Aviso de reposición. Mientras {@code resueltaEn} es null está abierta (una por producto). */
@Entity
@Table(name = "alertas_stock")
@Getter
@Setter
@NoArgsConstructor
public class AlertaStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoStock estado;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false)
    private LocalDateTime fecha;

    private LocalDateTime resueltaEn;

    public String mensaje() {
        String nivel = estado == EstadoStock.BAJO_MINIMO
                ? "bajo el stock mínimo (" + producto.getStockMinimo() + ")"
                : "en el punto de pedido (" + producto.getPuntoPedido() + ")";
        return "%s · %s: quedan %d unidades, %s".formatted(producto.getCodigo(), producto.getNombre(), stock, nivel);
    }
}
