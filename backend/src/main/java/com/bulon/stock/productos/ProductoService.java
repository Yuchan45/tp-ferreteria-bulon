package com.bulon.stock.productos;

import com.bulon.stock.common.exception.BusinessException;
import com.bulon.stock.common.exception.ResourceNotFoundException;
import com.bulon.stock.proveedores.ProveedorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final ProveedorService proveedorService;

    public List<ProductoDTO> listar() {
        return productoRepository.findAll().stream().map(ProductoDTO::from).toList();
    }

    public ProductoDTO obtener(Long id) {
        return ProductoDTO.from(buscarEntidad(id));
    }

    public List<ProductoDTO> listarBajoStockMinimo() {
        return productoRepository.findBajoStockMinimo().stream().map(ProductoDTO::from).toList();
    }

    @Transactional
    public ProductoDTO crear(ProductoDTO dto) {
        productoRepository.findByCodigo(dto.codigo()).ifPresent(p -> {
            throw new BusinessException("Ya existe un producto con código " + dto.codigo());
        });
        Producto producto = new Producto();
        aplicar(dto, producto);
        return ProductoDTO.from(productoRepository.save(producto));
    }

    @Transactional
    public ProductoDTO actualizar(Long id, ProductoDTO dto) {
        Producto producto = buscarEntidad(id);
        aplicar(dto, producto);
        return ProductoDTO.from(producto);
    }

    /** Uso interno de otros módulos (p. ej. movimientos de stock). */
    public Producto buscarEntidad(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));
    }

    private void aplicar(ProductoDTO dto, Producto producto) {
        producto.setCodigo(dto.codigo());
        producto.setDescripcion(dto.descripcion());
        producto.setPrecio(dto.precio());
        if (dto.stockMinimo() != null) {
            producto.setStockMinimo(dto.stockMinimo());
        }
        producto.setProveedor(dto.proveedorId() != null ? proveedorService.obtener(dto.proveedorId()) : null);
    }
}
