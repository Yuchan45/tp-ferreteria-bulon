package com.bulon.stock.productos;

import com.bulon.stock.categorias.Categoria;
import com.bulon.stock.common.auditoria.EntidadAuditada;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Artículo del catálogo. El stock no vive acá: ver {@code StockSucursal}. */
@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
public class Producto extends EntidadAuditada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** SKU. */
    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    /** Nombre corto para listados. */
    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidad_medida", nullable = false, length = 20)
    private UnidadMedida unidadMedida = UnidadMedida.UNIDAD;

    /** Precio de venta. */
    @Column(precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private boolean activo = true;
}
