package com.bulon.stock.productos;

import com.bulon.stock.categorias.CategoriaService;
import com.bulon.stock.common.exception.BusinessException;
import com.bulon.stock.common.exception.ResourceNotFoundException;
import com.bulon.stock.proveedores.Proveedor;
import com.bulon.stock.proveedores.ProveedorService;
import com.bulon.stock.stock.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final ProductoProveedorRepository productoProveedorRepository;
    private final ProveedorService proveedorService;
    private final CategoriaService categoriaService;
    private final StockService stockService;

    public List<ProductoDTO> listar() {
        Map<Long, BigDecimal> totales = stockService.totalesPorProducto();
        return productoRepository.findAll().stream()
                .map(p -> ProductoDTO.from(p, totales.getOrDefault(p.getId(), BigDecimal.ZERO)))
                .toList();
    }

    public ProductoDTO obtener(Long id) {
        return ProductoDTO.from(buscarEntidad(id), stockService.totalDe(id));
    }

    /** Crea el producto y su fila de stock (en 0) en cada sucursal. */
    @Transactional
    public ProductoDTO crear(ProductoDTO dto) {
        productoRepository.findByCodigo(dto.codigo()).ifPresent(p -> {
            throw new BusinessException("Ya existe un producto con código " + dto.codigo());
        });
        Producto producto = new Producto();
        aplicar(dto, producto);
        producto = productoRepository.save(producto);
        stockService.crearFilasPara(producto);
        return ProductoDTO.from(producto, BigDecimal.ZERO);
    }

    @Transactional
    public ProductoDTO actualizar(Long id, ProductoDTO dto) {
        Producto producto = buscarEntidad(id);
        productoRepository.findByCodigo(dto.codigo())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new BusinessException("Ya existe un producto con código " + dto.codigo());
                });
        aplicar(dto, producto);
        return ProductoDTO.from(producto, stockService.totalDe(id));
    }

    /** Proveedores del producto, con el preferido primero. */
    public List<ProductoProveedorDTO> listarProveedores(Long productoId) {
        buscarEntidad(productoId);
        return productoProveedorRepository.findByProductoId(productoId).stream()
                .sorted(Comparator.comparing(ProductoProveedor::isPreferido).reversed()
                        .thenComparing(pp -> pp.getProveedor().getRazonSocial()))
                .map(ProductoProveedorDTO::from)
                .toList();
    }

    /** Asocia (o actualiza) un proveedor del producto. Si queda preferido, desmarca al anterior. */
    @Transactional
    public ProductoProveedorDTO asignarProveedor(Long productoId, Long proveedorId, ProductoProveedorDTO condiciones) {
        Producto producto = buscarEntidad(productoId);
        Proveedor proveedor = proveedorService.obtener(proveedorId);

        if (condiciones.preferido()) {
            for (ProductoProveedor actual : productoProveedorRepository.findByIdProductoIdAndPreferidoTrue(productoId)) {
                if (!actual.getId().getProveedorId().equals(proveedorId)) {
                    actual.setPreferido(false);
                }
            }
            // Se escribe el cambio antes de marcar el nuevo, para no violar el índice único
            productoProveedorRepository.flush();
        }

        ProductoProveedor relacion = productoProveedorRepository
                .findById(new ProductoProveedorId(productoId, proveedorId))
                .orElseGet(() -> new ProductoProveedor(producto, proveedor));
        relacion.setPreferido(condiciones.preferido());
        relacion.setCodigoProveedor(condiciones.codigoProveedor());
        relacion.setPrecioCosto(condiciones.precioCosto());
        relacion.setPlazoEntregaDias(condiciones.plazoEntregaDias());
        return ProductoProveedorDTO.from(productoProveedorRepository.save(relacion));
    }

    /** Si el proveedor vende ese producto (regla para compras y devoluciones a proveedor). */
    public boolean esProveedorDe(Long productoId, Long proveedorId) {
        return productoProveedorRepository.existsByIdProductoIdAndIdProveedorId(productoId, proveedorId);
    }

    /** Uso interno de otros módulos (p. ej. movimientos de stock). */
    public Producto buscarEntidad(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));
    }

    private void aplicar(ProductoDTO dto, Producto producto) {
        producto.setCodigo(dto.codigo());
        producto.setNombre(dto.nombre());
        producto.setDescripcion(dto.descripcion());
        producto.setCategoria(dto.categoriaId() != null ? categoriaService.obtener(dto.categoriaId()) : null);
        if (dto.unidadMedida() != null) {
            producto.setUnidadMedida(dto.unidadMedida());
        }
        producto.setPrecio(dto.precio());
        if (dto.activo() != null) {
            producto.setActivo(dto.activo());
        }
    }
}
