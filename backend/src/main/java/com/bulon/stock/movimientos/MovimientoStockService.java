package com.bulon.stock.movimientos;

import com.bulon.stock.common.Busqueda;
import com.bulon.stock.common.exception.BusinessException;
import com.bulon.stock.movimientos.estrategias.EstrategiaMovimiento;
import com.bulon.stock.movimientos.estrategias.FabricaMovimientos;
import com.bulon.stock.productos.Producto;
import com.bulon.stock.productos.ProductoService;
import com.bulon.stock.proveedores.Proveedor;
import com.bulon.stock.proveedores.ProveedorService;
import com.bulon.stock.stock.StockService;
import com.bulon.stock.stock.StockSucursal;
import com.bulon.stock.sucursales.Sucursal;
import com.bulon.stock.sucursales.SucursalService;
import com.bulon.stock.usuarios.Usuario;
import com.bulon.stock.usuarios.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovimientoStockService {

    private final MovimientoStockRepository movimientoRepository;
    private final ProductoService productoService;
    private final SucursalService sucursalService;
    private final UsuarioService usuarioService;
    private final ProveedorService proveedorService;
    private final StockService stockService;
    private final FabricaMovimientos fabrica;

    /**
     * Historial de movimientos, del más reciente al más antiguo, con el stock que quedó
     * después de cada uno. El stock resultante no se guarda: se reconstruye hacia atrás
     * desde el stock actual de cada producto en cada sucursal. Por eso se recorre el
     * historial completo y los filtros de fecha, tipo y búsqueda se aplican al final.
     */
    public List<MovimientoDetalleDTO> listar(FiltroMovimientos filtro) {
        if (filtro.productoId() == null && filtro.sucursalId() == null) {
            throw new BusinessException("Indicá un producto o una sucursal para ver sus movimientos");
        }
        Map<String, BigDecimal> stock = new HashMap<>();
        (filtro.sucursalId() != null
                ? stockService.listarPorSucursal(filtro.sucursalId())
                : stockService.listarPorProducto(filtro.productoId()))
                .forEach(s -> stock.put(clave(s.productoId(), s.sucursalId()), s.cantidad()));

        List<MovimientoDetalleDTO> historial = new ArrayList<>();
        for (MovimientoStock m : movimientoRepository.findHistorial(filtro.productoId(), filtro.sucursalId())) {
            String clave = clave(m.getProducto().getId(), m.getSucursal().getId());
            BigDecimal despues = stock.getOrDefault(clave, BigDecimal.ZERO);
            BigDecimal efecto = efecto(m);
            historial.add(MovimientoDetalleDTO.from(m, efecto, despues));
            stock.put(clave, despues.subtract(efecto));   // lo que había antes de este movimiento
        }

        return historial.stream()
                .filter(m -> filtro.desde() == null || !m.fecha().toLocalDate().isBefore(filtro.desde()))
                .filter(m -> filtro.hasta() == null || !m.fecha().toLocalDate().isAfter(filtro.hasta()))
                .filter(m -> filtro.tipo() == null || m.tipo() == filtro.tipo())
                .filter(m -> Busqueda.coincide(filtro.q(), m.codigo(), m.nombre()))
                .toList();
    }

    /** Cuánto cambió el stock con este movimiento (+ ingreso, − salida), según su estrategia. */
    private BigDecimal efecto(MovimientoStock movimiento) {
        return fabrica.estrategiaPara(movimiento.getTipo()).delta(movimiento.getCantidad());
    }

    private static String clave(Long productoId, Long sucursalId) {
        return productoId + "/" + sucursalId;
    }

    /**
     * Las reglas que valen para todos los tipos quedan acá; las de cada tipo y su efecto
     * sobre el stock las resuelve su estrategia (Strategy), elegida por la fábrica (Simple Factory).
     */
    @Transactional
    public MovimientoStockDTO registrar(MovimientoStockDTO dto) {
        if (dto.usuarioId() == null) {
            throw new BusinessException("Falta el usuario responsable del movimiento");
        }
        Producto producto = productoService.buscarEntidad(dto.productoId());
        Sucursal sucursal = sucursalService.obtener(dto.sucursalId());
        Usuario usuario = usuarioService.buscarEntidad(dto.usuarioId());

        validarResponsable(usuario, sucursal);
        EstrategiaMovimiento estrategia = fabrica.estrategiaPara(dto.tipo());
        estrategia.validar(dto);
        validarFraccion(dto, producto);
        Proveedor proveedor = buscarProveedor(dto, producto);

        StockSucursal stock = stockService.buscarParaModificar(producto, sucursal);
        // StockService avisa a sus observadores (alertas → notificaciones)
        stockService.aplicar(stock, estrategia.delta(dto.cantidad()));

        MovimientoStock movimiento = fabrica.crear(dto, producto, sucursal, usuario, proveedor);
        return MovimientoStockDTO.from(movimientoRepository.save(movimiento));
    }

    private void validarResponsable(Usuario usuario, Sucursal sucursal) {
        if (!usuario.isActivo()) {
            throw new BusinessException("El usuario " + usuario.getUsername() + " está inactivo");
        }
        if (!usuario.puedeOperar(sucursal)) {
            throw new BusinessException("El usuario " + usuario.getUsername()
                    + " solo puede registrar movimientos de su sucursal");
        }
    }

    private void validarFraccion(MovimientoStockDTO dto, Producto producto) {
        boolean tieneFraccion = dto.cantidad().stripTrailingZeros().scale() > 0;
        if (tieneFraccion && !producto.getUnidadMedida().isFraccionable()) {
            throw new BusinessException("El producto " + producto.getCodigo() + " se maneja por "
                    + producto.getUnidadMedida().name().toLowerCase() + ": la cantidad debe ser entera");
        }
    }

    /** La estrategia ya validó si el tipo lleva proveedor; acá se comprueba que venda el producto. */
    private Proveedor buscarProveedor(MovimientoStockDTO dto, Producto producto) {
        if (dto.proveedorId() == null) {
            return null;
        }
        if (!productoService.esProveedorDe(producto.getId(), dto.proveedorId())) {
            throw new BusinessException("El proveedor " + dto.proveedorId()
                    + " no vende el producto " + producto.getCodigo());
        }
        Proveedor proveedor = proveedorService.obtener(dto.proveedorId());
        // A un proveedor desactivado ya no se le compra; devolverle mercadería sí se puede
        if (dto.tipo() == TipoMovimiento.INGRESO_COMPRA && !proveedor.isActivo()) {
            throw new BusinessException("El proveedor " + proveedor.getRazonSocial() + " está inactivo");
        }
        return proveedor;
    }
}
