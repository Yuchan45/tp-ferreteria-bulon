package com.bulon.stock.compras;

import com.bulon.stock.common.exception.BusinessException;
import com.bulon.stock.proveedores.Proveedor;
import com.bulon.stock.ubicaciones.Ubicacion;
import com.bulon.stock.usuarios.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ordenes_compra")
@Getter
@Setter
@NoArgsConstructor
public class OrdenCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOrdenCompra estado = EstadoOrdenCompra.BORRADOR;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaRecepcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ubicacion_recepcion_id")
    private Ubicacion ubicacionRecepcion;

    private String observacion;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrdenCompraItem> items = new ArrayList<>();

    public void pasarA(EstadoOrdenCompra destino) {
        if (!estado.puedePasarA(destino)) {
            throw new BusinessException("La orden %d está %s y no puede pasar a %s".formatted(id, estado, destino));
        }
        estado = destino;
    }
}
