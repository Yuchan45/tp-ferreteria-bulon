package com.bulon.stock.productos;

import com.bulon.stock.proveedores.Proveedor;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/** El stock no vive acá: está por ubicación en {@code stock.Existencia}. */
@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 200)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidad_medida", nullable = false, length = 20)
    private UnidadMedida unidadMedida = UnidadMedida.UNIDAD;

    @Column(precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private int stockMinimo;

    @Column(nullable = false)
    private int puntoPedido;

    @Column(nullable = false)
    private boolean activo = true;

    @ManyToMany
    @JoinTable(name = "producto_proveedor",
            joinColumns = @JoinColumn(name = "producto_id"),
            inverseJoinColumns = @JoinColumn(name = "proveedor_id"))
    private Set<Proveedor> proveedores = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_preferido_id")
    private Proveedor proveedorPreferido;

    public EstadoStock estadoStock(int stockTotal) {
        return EstadoStock.de(stockTotal, stockMinimo, puntoPedido);
    }
}
