package com.bulon.stock.stock;

import com.bulon.stock.common.exception.BusinessException;
import com.bulon.stock.common.exception.ResourceNotFoundException;
import com.bulon.stock.productos.Producto;
import com.bulon.stock.sucursales.Sucursal;
import com.bulon.stock.sucursales.SucursalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final StockSucursalRepository stockRepository;
    private final SucursalService sucursalService;

    /** Detalle del stock de un producto en cada sucursal. */
    public List<StockSucursalDTO> listarPorProducto(Long productoId) {
        return stockRepository.findByProductoId(productoId).stream().map(StockSucursalDTO::from).toList();
    }

    /** Stock de todos los productos en una sucursal. */
    public List<StockSucursalDTO> listarPorSucursal(Long sucursalId) {
        sucursalService.obtener(sucursalId);
        return stockRepository.findBySucursalId(sucursalId).stream().map(StockSucursalDTO::from).toList();
    }

    /** Productos en o por debajo del punto de pedido (sucursalId null = todas). */
    public List<StockSucursalDTO> listarAReponer(Long sucursalId) {
        return stockRepository.findAReponer(sucursalId).stream().map(StockSucursalDTO::from).toList();
    }

    /** Stock total por producto (suma de todas las sucursales). */
    public Map<Long, BigDecimal> totalesPorProducto() {
        Map<Long, BigDecimal> totales = new HashMap<>();
        for (Object[] fila : stockRepository.sumarPorProducto()) {
            totales.put((Long) fila[0], (BigDecimal) fila[1]);
        }
        return totales;
    }

    public BigDecimal totalDe(Long productoId) {
        return totalesPorProducto().getOrDefault(productoId, BigDecimal.ZERO);
    }

    /** Al dar de alta un producto se crea su fila de stock (en 0) en cada sucursal. */
    @Transactional
    public void crearFilasPara(Producto producto) {
        for (Sucursal sucursal : sucursalService.listar()) {
            StockSucursalId id = new StockSucursalId(producto.getId(), sucursal.getId());
            if (!stockRepository.existsById(id)) {
                stockRepository.save(new StockSucursal(producto, sucursal));
            }
        }
    }

    @Transactional
    public StockSucursalDTO configurarUmbrales(Long productoId, Long sucursalId, UmbralesRequest request) {
        if (request.puntoPedido().compareTo(request.stockMinimo()) < 0) {
            throw new BusinessException("El punto de pedido no puede ser menor que el stock mínimo");
        }
        StockSucursal stock = stockRepository.findById(new StockSucursalId(productoId, sucursalId))
                .orElseThrow(() -> new ResourceNotFoundException("Stock del producto " + productoId + " en sucursal", sucursalId));
        stock.setStockMinimo(request.stockMinimo());
        stock.setPuntoPedido(request.puntoPedido());
        return StockSucursalDTO.from(stock);
    }

    /**
     * Uso interno del módulo de movimientos: la fila que hay que modificar.
     * Si faltara (p. ej. una sucursal nueva), se crea en 0.
     */
    @Transactional
    public StockSucursal buscarParaModificar(Producto producto, Sucursal sucursal) {
        return stockRepository.findById(new StockSucursalId(producto.getId(), sucursal.getId()))
                .orElseGet(() -> stockRepository.save(new StockSucursal(producto, sucursal)));
    }
}
