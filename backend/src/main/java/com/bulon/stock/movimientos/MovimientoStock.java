package com.bulon.stock.movimientos;

import com.bulon.stock.productos.Producto;
import com.bulon.stock.proveedores.Proveedor;
import com.bulon.stock.sucursales.Sucursal;
import com.bulon.stock.usuarios.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Registro inmutable de un cambio de stock: nunca se edita ni se borra. */
@Entity
@Table(name = "movimientos_stock")
@Getter
@Setter
@NoArgsConstructor
public class MovimientoStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    /** Responsable del movimiento (trazabilidad). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private TipoMovimiento tipo;

    /** Positiva, salvo en AJUSTE, que lleva signo. */
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(length = 255)
    private String motivo;

    @Column(length = 500)
    private String observaciones;

    /** Nº de remito, factura o ticket. */
    @Column(length = 50)
    private String referencia;

    /** Solo en compras y devoluciones a proveedor. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    /** Solicitud de reposición que originó el ingreso, si la hay. */
    @Column(name = "solicitud_id")
    private Long solicitudId;
}
