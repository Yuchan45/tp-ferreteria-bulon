package com.bulon.stock.movimientos;

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
    private final FabricaMovimientos fabrica;

    public List<MovimientoStockDTO> listarPorProducto(Long productoId, Long sucursalId) {
        List<MovimientoStock> movimientos = sucursalId == null
                ? movimientoRepository.findByProductoIdOrderByFechaDescIdDesc(productoId)
                : movimientoRepository.findByProductoIdAndSucursalIdOrderByFechaDescIdDesc(productoId, sucursalId);
        return movimientos.stream().map(MovimientoStockDTO::from).toList();
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
        stock.aplicar(estrategia.delta(dto.cantidad()));

        MovimientoStock movimiento = fabrica.crear(dto, producto, sucursal, usuario, proveedor);

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
        return proveedorService.obtener(dto.proveedorId());
    }
}
