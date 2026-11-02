package com.bulon.stock.movimientos.estrategias;

import com.bulon.stock.movimientos.MovimientoStock;
import com.bulon.stock.movimientos.MovimientoStockDTO;
import com.bulon.stock.movimientos.TipoMovimiento;
import com.bulon.stock.productos.Producto;
import com.bulon.stock.proveedores.Proveedor;
import com.bulon.stock.sucursales.Sucursal;
import com.bulon.stock.usuarios.Usuario;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Patrón Simple Factory: una sola clase que, según el tipo, entrega la estrategia que
 * corresponde y crea el movimiento. No es el Factory Method de GoF (no delega la creación
 * en subclases): alcanza con una fábrica porque todos los tipos se guardan en la misma entidad.
 * Es el único lugar donde se decide por tipo, y lo hace sin switch: Spring le pasa todas
 * las estrategias ({@code @Component}) y se indexan por {@link EstrategiaMovimiento#tipo()}.
 */
@Component
public class FabricaMovimientos {

    private final Map<TipoMovimiento, EstrategiaMovimiento> estrategias = new EnumMap<>(TipoMovimiento.class);

    public FabricaMovimientos(List<EstrategiaMovimiento> todas) {
        for (EstrategiaMovimiento estrategia : todas) {
            if (estrategias.put(estrategia.tipo(), estrategia) != null) {
                throw new IllegalStateException("Hay dos estrategias para " + estrategia.tipo());
            }
        }
        // Si se agrega un tipo al enum sin su estrategia, la aplicación no arranca
        for (TipoMovimiento tipo : TipoMovimiento.values()) {
            if (!estrategias.containsKey(tipo)) {
                throw new IllegalStateException("Falta la estrategia para " + tipo);
            }
        }
    }

    public EstrategiaMovimiento estrategiaPara(TipoMovimiento tipo) {
        return estrategias.get(tipo);
    }

    /** Crea el movimiento listo para guardar, con los datos ya validados. */
    public MovimientoStock crear(MovimientoStockDTO dto, Producto producto, Sucursal sucursal,
                                 Usuario usuario, Proveedor proveedor) {
        MovimientoStock movimiento = new MovimientoStock();
        movimiento.setProducto(producto);
        movimiento.setSucursal(sucursal);
        movimiento.setUsuario(usuario);
        movimiento.setTipo(dto.tipo());
        movimiento.setCantidad(dto.cantidad());
        movimiento.setMotivo(blancoANull(dto.motivo()));
        movimiento.setObservaciones(blancoANull(dto.observaciones()));
        movimiento.setReferencia(blancoANull(dto.referencia()));
        movimiento.setProveedor(proveedor);
        return movimiento;
    }

    private static String blancoANull(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
