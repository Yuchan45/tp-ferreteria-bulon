package com.bulon.stock.movimientos;

import com.bulon.stock.common.exception.BusinessException;
import com.bulon.stock.productos.Producto;
import com.bulon.stock.productos.ProductoService;
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

    public List<MovimientoStockDTO> listarPorProducto(Long productoId) {
        return movimientoRepository.findByProductoIdOrderByFechaDesc(productoId).stream()
                .map(MovimientoStockDTO::from)
                .toList();
    }

    @Transactional
    public MovimientoStockDTO registrar(MovimientoStockDTO dto) {
        Producto producto = productoService.buscarEntidad(dto.productoId());

        int delta = switch (dto.tipo()) {
            case ENTRADA -> positiva(dto.cantidad());
            case SALIDA -> -positiva(dto.cantidad());
            case AJUSTE -> dto.cantidad();
        };

        int nuevoStock = producto.getStockActual() + delta;
        if (nuevoStock < 0) {
            throw new BusinessException("Stock insuficiente para el producto " + producto.getCodigo());
        }
        producto.setStockActual(nuevoStock);

        MovimientoStock movimiento = new MovimientoStock();
        movimiento.setProducto(producto);
        movimiento.setTipo(dto.tipo());
        movimiento.setCantidad(dto.cantidad());
        movimiento.setObservacion(dto.observacion());

        // TODO: notificar alerta de reposición cuando producto.requiereReposicion()
        return MovimientoStockDTO.from(movimientoRepository.save(movimiento));
    }

    private int positiva(int cantidad) {
        if (cantidad <= 0) {
            throw new BusinessException("La cantidad debe ser mayor a cero");
        }
        return cantidad;
    }
}
