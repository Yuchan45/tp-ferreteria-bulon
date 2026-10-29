package com.bulon.stock.stock;

import com.bulon.stock.common.auditoria.EntidadAuditada;
import com.bulon.stock.common.exception.BusinessException;
import com.bulon.stock.productos.Producto;
import com.bulon.stock.sucursales.Sucursal;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Existencias de un producto en una sucursal, con sus dos umbrales.
 * La cantidad solo cambia a través de {@link #aplicar(BigDecimal)}, que llama
 * el servicio de movimientos: así ningún cambio de stock queda sin registrar.
 */
@Entity
@Table(name = "stock_sucursal")
@Getter
@Setter
@NoArgsConstructor
public class StockSucursal extends EntidadAuditada {

    @EmbeddedId
    private StockSucursalId id;

    @MapsId("productoId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @MapsId("sucursalId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    @Column(nullable = false, precision = 12, scale = 3)
    @Setter(AccessLevel.NONE)
    private BigDecimal cantidad = BigDecimal.ZERO;

    /** Piso de seguridad: al llegar acá la situación es crítica. */
    @Column(name = "stock_minimo", nullable = false, precision = 12, scale = 3)
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    /** Cuándo conviene pedir (siempre ≥ mínimo). */
    @Column(name = "punto_pedido", nullable = false, precision = 12, scale = 3)
    private BigDecimal puntoPedido = BigDecimal.ZERO;

    /**
     * Bloqueo optimista: si dos operaciones leen la misma fila y ambas la modifican,
     * la segunda falla en lugar de pisar el cambio de la primera.
     */
    @Version
    @Column(nullable = false)
    private Integer version;

    public StockSucursal(Producto producto, Sucursal sucursal) {
        this.id = new StockSucursalId(producto.getId(), sucursal.getId());
        this.producto = producto;
        this.sucursal = sucursal;
    }

    /** Suma (o resta, si es negativo) al stock. Nunca puede quedar negativo. */
    public void aplicar(BigDecimal delta) {
        BigDecimal nueva = cantidad.add(delta);
        if (nueva.signum() < 0) {
            throw new BusinessException("Stock insuficiente para el producto " + producto.getCodigo()
                    + " en " + sucursal.getNombre() + ": hay " + cantidad.stripTrailingZeros().toPlainString());
        }
        cantidad = nueva;
    }

    public EstadoStock estado() {
        if (cantidad.compareTo(stockMinimo) <= 0) {
            return EstadoStock.CRITICO;
        }
        if (cantidad.compareTo(puntoPedido) <= 0) {
            return EstadoStock.REPONER;
        }
        return EstadoStock.OK;
    }
}
