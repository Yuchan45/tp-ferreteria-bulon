package com.bulon.stock.movimientos;

import com.bulon.stock.common.exception.BusinessException;
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
import java.util.List;

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

    public List<MovimientoStockDTO> listarPorProducto(Long productoId, Long sucursalId) {
        List<MovimientoStock> movimientos = sucursalId == null
                ? movimientoRepository.findByProductoIdOrderByFechaDescIdDesc(productoId)
                : movimientoRepository.findByProductoIdAndSucursalIdOrderByFechaDescIdDesc(productoId, sucursalId);
        return movimientos.stream().map(MovimientoStockDTO::from).toList();
    }

    @Transactional
    public MovimientoStockDTO registrar(MovimientoStockDTO dto) {
        if (dto.usuarioId() == null) {
            throw new BusinessException("Falta el usuario responsable del movimiento");
        }
        Producto producto = productoService.buscarEntidad(dto.productoId());
        Sucursal sucursal = sucursalService.obtener(dto.sucursalId());
        Usuario usuario = usuarioService.buscarEntidad(dto.usuarioId());

        validarResponsable(usuario, sucursal);
        validarCantidad(dto, producto);
        validarMotivo(dto);
        Proveedor proveedor = validarProveedor(dto, producto);

        // TODO (patrón Strategy): cada tipo de movimiento tendrá su propia estrategia
        // de validación y efecto sobre el stock, en lugar de este switch.
        BigDecimal delta = switch (dto.tipo()) {
            case INGRESO_COMPRA, DEVOLUCION_CLIENTE -> dto.cantidad();
            case SALIDA_VENTA, DEVOLUCION_PROVEEDOR, ROTURA -> dto.cantidad().negate();
            case AJUSTE -> dto.cantidad();
        };

        StockSucursal stock = stockService.buscarParaModificar(producto, sucursal);
        stock.aplicar(delta);

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

        // TODO (patrón Observer): si stock.estado() no es OK, avisar para crear la alerta
        // de reposición y las notificaciones.
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

    private void validarCantidad(MovimientoStockDTO dto, Producto producto) {
        int signo = dto.cantidad().signum();
        if (dto.tipo() == TipoMovimiento.AJUSTE ? signo == 0 : signo <= 0) {
            throw new BusinessException(dto.tipo() == TipoMovimiento.AJUSTE
                    ? "El ajuste no puede ser 0"
                    : "La cantidad debe ser mayor a cero");
        }
        boolean tieneFraccion = dto.cantidad().stripTrailingZeros().scale() > 0;
        if (tieneFraccion && !producto.getUnidadMedida().isFraccionable()) {
            throw new BusinessException("El producto " + producto.getCodigo() + " se maneja por "
                    + producto.getUnidadMedida().name().toLowerCase() + ": la cantidad debe ser entera");
        }
    }

    private void validarMotivo(MovimientoStockDTO dto) {
        if (dto.tipo().requiereMotivo() && blancoANull(dto.motivo()) == null) {
            throw new BusinessException("El motivo es obligatorio para " + dto.tipo());
        }
    }

    private Proveedor validarProveedor(MovimientoStockDTO dto, Producto producto) {
        if (!dto.tipo().requiereProveedor()) {
            if (dto.proveedorId() != null) {
                throw new BusinessException("El proveedor solo se indica en compras y devoluciones a proveedor");
            }
            return null;
        }
        if (dto.proveedorId() == null) {
            throw new BusinessException("El proveedor es obligatorio para " + dto.tipo());
        }
        if (!productoService.esProveedorDe(producto.getId(), dto.proveedorId())) {
            throw new BusinessException("El proveedor " + dto.proveedorId()
                    + " no vende el producto " + producto.getCodigo());
        }
        return proveedorService.obtener(dto.proveedorId());
    }

    private static String blancoANull(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
