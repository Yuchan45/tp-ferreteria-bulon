package com.bulon.stock.stock;

import com.bulon.stock.common.exception.BusinessException;
import com.bulon.stock.productos.Producto;
import com.bulon.stock.ubicaciones.Ubicacion;
import com.bulon.stock.ubicaciones.UbicacionDTO;
import com.bulon.stock.ubicaciones.UbicacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final ExistenciaRepository existenciaRepository;
    private final UbicacionService ubicacionService;

    /** Stock de cada producto en todas las ubicaciones (con 0 donde no hay existencia). */
    public Map<Long, List<ExistenciaDTO>> existencias(Collection<Long> productoIds) {
        Map<Long, Map<Long, Integer>> cantidades = existenciaRepository.findByProductoIdIn(productoIds).stream()
                .collect(Collectors.groupingBy(e -> e.getProducto().getId(),
                        Collectors.toMap(e -> e.getUbicacion().getId(), Existencia::getCantidad)));
        List<UbicacionDTO> ubicaciones = ubicacionService.listar();

        Map<Long, List<ExistenciaDTO>> resultado = new HashMap<>();
        for (Long productoId : productoIds) {
            Map<Long, Integer> porUbicacion = cantidades.getOrDefault(productoId, Map.of());
            resultado.put(productoId, ubicaciones.stream()
                    .map(u -> new ExistenciaDTO(u.id(), u.nombre(), porUbicacion.getOrDefault(u.id(), 0)))
                    .toList());
        }
        return resultado;
    }

    public List<ExistenciaDTO> existencias(Long productoId) {
        return existencias(List.of(productoId)).get(productoId);
    }

    public int total(Long productoId) {
        return existencias(productoId).stream().mapToInt(ExistenciaDTO::cantidad).sum();
    }

    public int disponible(Producto producto, Ubicacion ubicacion) {
        return existenciaRepository.findByProductoIdAndUbicacionId(producto.getId(), ubicacion.getId())
                .map(Existencia::getCantidad)
                .orElse(0);
    }

    public void verificarDisponible(Producto producto, Ubicacion ubicacion, int cantidad) {
        int disponible = disponible(producto, ubicacion);
        if (disponible < cantidad) {
            throw new BusinessException("Stock insuficiente de %s en %s: disponible %d, solicitado %d"
                    .formatted(producto.getCodigo(), ubicacion.getNombre(), disponible, cantidad));
        }
    }

    /** Única operación que modifica existencias; la invocan las estrategias de movimiento. */
    @Transactional
    public void mover(Producto producto, Ubicacion ubicacion, int delta) {
        Existencia existencia = existenciaRepository
                .findByProductoIdAndUbicacionId(producto.getId(), ubicacion.getId())
                .orElseGet(() -> new Existencia(producto, ubicacion));
        existencia.sumar(delta);
        existenciaRepository.save(existencia);
    }

    public List<UnidadesPorUbicacion> unidadesPorUbicacion() {
        return existenciaRepository.unidadesPorUbicacion();
    }
}
