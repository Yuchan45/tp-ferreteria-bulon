package com.bulon.stock.productos;

import com.bulon.stock.stock.StockService;
import com.bulon.stock.stock.StockSucursalDTO;
import com.bulon.stock.stock.UmbralesRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;
    private final StockService stockService;

    @GetMapping
    public List<ProductoDTO> listar() {
        return productoService.listar();
    }

    @GetMapping("/{id}")
    public ProductoDTO obtener(@PathVariable Long id) {
        return productoService.obtener(id);
    }

    /** Productos en o por debajo del punto de pedido, en una sucursal o en todas. */
    @GetMapping("/bajo-stock")
    public List<StockSucursalDTO> bajoStock(@RequestParam(required = false) Long sucursalId) {
        return stockService.listarAReponer(sucursalId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ProductoDTO crear(@Valid @RequestBody ProductoDTO dto) {
        return productoService.crear(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ProductoDTO actualizar(@PathVariable Long id, @Valid @RequestBody ProductoDTO dto) {
        return productoService.actualizar(id, dto);
    }

    /** Stock del producto en cada sucursal. */
    @GetMapping("/{id}/stock")
    public List<StockSucursalDTO> stock(@PathVariable Long id) {
        productoService.buscarEntidad(id);
        return stockService.listarPorProducto(id);
    }

    /** Configura el stock mínimo y el punto de pedido del producto en una sucursal. */
    @PutMapping("/{id}/stock/{sucursalId}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public StockSucursalDTO configurarUmbrales(@PathVariable Long id, @PathVariable Long sucursalId,
                                               @Valid @RequestBody UmbralesRequest request) {
        return stockService.configurarUmbrales(id, sucursalId, request);
    }

    @GetMapping("/{id}/proveedores")
    public List<ProductoProveedorDTO> proveedores(@PathVariable Long id) {
        return productoService.listarProveedores(id);
    }

    /** Asocia un proveedor al producto (o actualiza sus condiciones). */
    @PutMapping("/{id}/proveedores/{proveedorId}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ProductoProveedorDTO asignarProveedor(@PathVariable Long id, @PathVariable Long proveedorId,
                                                 @Valid @RequestBody ProductoProveedorDTO condiciones) {
        return productoService.asignarProveedor(id, proveedorId, condiciones);
    }
}
