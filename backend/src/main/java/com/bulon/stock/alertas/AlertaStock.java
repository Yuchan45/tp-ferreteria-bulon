package com.bulon.stock.alertas;

import com.bulon.stock.productos.Producto;
import com.bulon.stock.stock.StockSucursal;
import com.bulon.stock.sucursales.Sucursal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Aviso de que un producto necesita reposición en una sucursal. Hay a lo sumo una
 * alerta activa (ABIERTA) por producto y sucursal (índice uq_alertas_activa).
 */
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NivelAlerta nivel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoAlerta estado = EstadoAlerta.ABIERTA;

    /** Valores vigentes al generarse (o al subir a CRITICO). */
    @Column(name = "cantidad_al_generar", nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidadAlGenerar;

    @Column(name = "punto_pedido", nullable = false, precision = 12, scale = 3)
    private BigDecimal puntoPedido;

    @Column(name = "stock_minimo", nullable = false, precision = 12, scale = 3)
    private BigDecimal stockMinimo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime creada = LocalDateTime.now();

    private LocalDateTime resuelta;

    public AlertaStock(StockSucursal stock, NivelAlerta nivel) {
        this.producto = stock.getProducto();
        this.sucursal = stock.getSucursal();
        actualizar(stock, nivel);
    }

    /** Guarda el nivel y la foto del stock que lo provocó. */
    public void actualizar(StockSucursal stock, NivelAlerta nivel) {
        this.nivel = nivel;
        this.cantidadAlGenerar = stock.getCantidad();
        this.puntoPedido = stock.getPuntoPedido();
        this.stockMinimo = stock.getStockMinimo();
    }

    public void resolver() {
        estado = EstadoAlerta.RESUELTA;
        resuelta = LocalDateTime.now();
    }
}
