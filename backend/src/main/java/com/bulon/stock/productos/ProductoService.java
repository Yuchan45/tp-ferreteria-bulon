package com.bulon.stock.productos;

import com.bulon.stock.common.exception.BusinessException;
import com.bulon.stock.common.exception.ResourceNotFoundException;
import com.bulon.stock.movimientos.StockObserver;
import com.bulon.stock.proveedores.Proveedor;
import com.bulon.stock.proveedores.ProveedorService;
import com.bulon.stock.stock.ExistenciaDTO;
import com.bulon.stock.stock.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaService categoriaService;
    private final ProveedorService proveedorService;
    private final StockService stockService;
    private final List<StockObserver> observadores;

    public Page<ProductoDTO> listar(String texto, Long categoriaId, Boolean activo, boolean reposicion, Pageable pageable) {
        String patron = texto == null || texto.isBlank() ? null : "%" + texto.trim().toLowerCase() + "%";
        Page<Producto> pagina = productoRepository.buscar(patron, categoriaId, activo, reposicion, pageable);
        Map<Long, List<ExistenciaDTO>> existencias = stockService.existencias(
                pagina.getContent().stream().map(Producto::getId).toList());
        return pagina.map(p -> ProductoDTO.from(p, existencias.get(p.getId())));
    }

    public List<ProductoDTO> pendientesReposicion() {
        List<Producto> productos = productoRepository.findPendientesReposicion();
        Map<Long, List<ExistenciaDTO>> existencias = stockService.existencias(
                productos.stream().map(Producto::getId).toList());
        return productos.stream().map(p -> ProductoDTO.from(p, existencias.get(p.getId()))).toList();
    }

    public long contarPendientesReposicion() {
        return productoRepository.contarPendientesReposicion();
    }

    public long contarBajoMinimo() {
        return productoRepository.contarBajoMinimo();
    }

    public long contarActivos() {
        return productoRepository.countByActivoTrue();
    }

    public ProductoDTO obtener(Long id) {
        return aDTO(buscarEntidad(id));
    }

    public ProductoDTO aDTO(Producto producto) {
        return ProductoDTO.from(producto, stockService.existencias(producto.getId()));
    }

    @Transactional
    public ProductoDTO crear(ProductoRequest request) {
        verificarCodigoLibre(request.codigo(), null);
        Producto producto = new Producto();
        aplicar(request, producto);
        return notificar(productoRepository.save(producto));
    }

    @Transactional
    public ProductoDTO actualizar(Long id, ProductoRequest request) {
        Producto producto = buscarEntidad(id);
        verificarCodigoLibre(request.codigo(), id);
        aplicar(request, producto);
        return notificar(producto);
    }

    /** Alta o cambio de mínimo, punto de pedido o activo: puede abrir o cerrar la alerta sin movimiento. */
    private ProductoDTO notificar(Producto producto) {
        ProductoDTO dto = aDTO(producto);
        observadores.forEach(o -> o.stockActualizado(producto, dto.stockTotal()));
        return dto;
    }

    /** Uso interno de otros módulos (movimientos, compras, reportes). */
    public Producto buscarEntidad(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));
    }

    private void verificarCodigoLibre(String codigo, Long idActual) {
        productoRepository.findByCodigo(codigo)
                .filter(p -> !p.getId().equals(idActual))
                .ifPresent(p -> {
                    throw new BusinessException("Ya existe un producto con código " + codigo);
                });
    }

    private void aplicar(ProductoRequest request, Producto producto) {
        if (request.puntoPedido() < request.stockMinimo()) {
            throw new BusinessException("El punto de pedido no puede ser menor que el stock mínimo");
        }
        Set<Long> proveedorIds = new HashSet<>(Objects.requireNonNullElse(request.proveedorIds(), List.of()));
        if (request.proveedorPreferidoId() != null) {
            proveedorIds.add(request.proveedorPreferidoId());
        }
        Set<Proveedor> proveedores = new HashSet<>();
        proveedorIds.forEach(id -> proveedores.add(proveedorService.buscarEntidad(id)));

        producto.setCodigo(request.codigo().trim());
        producto.setNombre(request.nombre().trim());
        producto.setDescripcion(request.descripcion());
        producto.setCategoria(categoriaService.buscarEntidad(request.categoriaId()));
        producto.setUnidadMedida(request.unidadMedida());
        producto.setPrecio(request.precio());
        producto.setStockMinimo(request.stockMinimo());
        producto.setPuntoPedido(request.puntoPedido());
        producto.setActivo(request.activo() == null || request.activo());
        producto.setProveedores(proveedores);
        producto.setProveedorPreferido(request.proveedorPreferidoId() != null
                ? proveedorService.buscarEntidad(request.proveedorPreferidoId())
                : null);
    }
}
