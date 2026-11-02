package com.bulon.stock.productos;

import com.bulon.stock.proveedores.Proveedor;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Qué proveedor vende un producto (relación N:M), con sus condiciones. */
@Entity
@Table(name = "producto_proveedor")
@Getter
@Setter
@NoArgsConstructor
public class ProductoProveedor {

    @EmbeddedId
    private ProductoProveedorId id;

    @MapsId("productoId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @MapsId("proveedorId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    /** A lo sumo un preferido por producto (índice único filtrado en la base). */
    @Column(nullable = false)
    private boolean preferido;

    /** Cómo identifica el proveedor al artículo. */
    @Column(name = "codigo_proveedor", length = 50)
    private String codigoProveedor;

    @Column(name = "precio_costo", precision = 12, scale = 2)
    private BigDecimal precioCosto;

    @Column(name = "plazo_entrega_dias")
    private Integer plazoEntregaDias;

    public ProductoProveedor(Producto producto, Proveedor proveedor) {
        this.id = new ProductoProveedorId(producto.getId(), proveedor.getId());
        this.producto = producto;
        this.proveedor = proveedor;
    }
}
