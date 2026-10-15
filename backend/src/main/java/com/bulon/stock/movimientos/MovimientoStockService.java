package com.bulon.stock.movimientos;

import com.bulon.stock.common.exception.BusinessException;
import com.bulon.stock.movimientos.factory.DatosMovimiento;
import com.bulon.stock.movimientos.factory.MovimientoStockCreator;
import com.bulon.stock.movimientos.strategy.MovimientoStrategy;
import com.bulon.stock.productos.Producto;
import com.bulon.stock.productos.ProductoService;
import com.bulon.stock.stock.StockService;
import com.bulon.stock.ubicaciones.UbicacionService;
import com.bulon.stock.usuarios.Usuario;
import com.bulon.stock.usuarios.UsuarioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Service
@Transactional(readOnly = true)
public class MovimientoStockService {

    private final MovimientoStockRepository movimientoRepository;
    private final ProductoService productoService;
    private final UbicacionService ubicacionService;
    private final UsuarioService usuarioService;
    private final StockService stockService;
    private final Map<TipoMovimiento, MovimientoStockCreator> creators;
    private final Map<TipoMovimiento, MovimientoStrategy> estrategias;
    private final List<StockObserver> observadores;

    public MovimientoStockService(MovimientoStockRepository movimientoRepository,
                                  ProductoService productoService,
                                  UbicacionService ubicacionService,
                                  UsuarioService usuarioService,
                                  StockService stockService,
                                  List<MovimientoStockCreator> creators,
                                  List<MovimientoStrategy> estrategias,
                                  List<StockObserver> observadores) {
        this.movimientoRepository = movimientoRepository;
        this.productoService = productoService;
        this.ubicacionService = ubicacionService;
        this.usuarioService = usuarioService;
        this.stockService = stockService;
        this.creators = porTipo(creators, MovimientoStockCreator::tipos, "creator");
        this.estrategias = porTipo(estrategias, MovimientoStrategy::tipos, "estrategia");
        this.observadores = observadores;
    }

    public Page<MovimientoDTO> listar(FiltroMovimientos f, Pageable pageable) {
        return movimientoRepository.buscar(f.productoId(), f.tipo(), f.usuarioId(), f.ubicacionId(),
                        f.desde() != null ? f.desde().atStartOfDay() : null,
                        f.hasta() != null ? f.hasta().plusDays(1).atStartOfDay() : null,
                        pageable)
                .map(MovimientoDTO::from);
    }

    public long contarDesde(LocalDate dia) {
        return movimientoRepository.countByFechaGreaterThanEqual(dia.atStartOfDay());
    }

    @Transactional
    public MovimientoDTO registrar(MovimientoRequest request, String username) {
        Usuario usuario = usuarioService.buscarPorUsername(username);
        if (!request.tipo().permitidoPara(usuario.getRol())) {
            throw new AccessDeniedException("El rol %s no puede registrar movimientos de tipo %s"
                    .formatted(usuario.getRol(), request.tipo()));
        }
        Producto producto = productoService.buscarEntidad(request.productoId());
        if (!producto.isActivo()) {
            throw new BusinessException("El producto " + producto.getCodigo() + " está inactivo");
        }

        MovimientoStock movimiento = creators.get(request.tipo()).registrar(new DatosMovimiento(
                request.tipo(),
                producto,
                request.cantidad(),
                ubicacionService.buscarEntidad(request.ubicacionId()),
                request.ubicacionDestinoId() != null ? ubicacionService.buscarEntidad(request.ubicacionDestinoId()) : null,
                usuario,
                request.motivo(),
                request.observacion()));

        MovimientoStrategy estrategia = estrategias.get(request.tipo());
        estrategia.validar(movimiento);
        estrategia.actualizarStock(movimiento);
        MovimientoStock guardado = movimientoRepository.save(movimiento);

        int stockTotal = stockService.total(producto.getId());
        observadores.forEach(o -> o.stockActualizado(producto, stockTotal));
        return MovimientoDTO.from(guardado);
    }

    /** Falla al arrancar si a algún tipo le falta (o le sobra) un creator o una estrategia. */
    private static <T> Map<TipoMovimiento, T> porTipo(List<T> beans, Function<T, Set<TipoMovimiento>> tipos,
                                                      String rol) {
        Map<TipoMovimiento, T> mapa = new EnumMap<>(TipoMovimiento.class);
        for (T bean : beans) {
            for (TipoMovimiento tipo : tipos.apply(bean)) {
                if (mapa.put(tipo, bean) != null) {
                    throw new IllegalStateException("Hay más de un " + rol + " para " + tipo);
                }
            }
        }
        for (TipoMovimiento tipo : TipoMovimiento.values()) {
            if (!mapa.containsKey(tipo)) {
                throw new IllegalStateException("Falta un " + rol + " para " + tipo);
            }
        }
        return mapa;
    }
}
