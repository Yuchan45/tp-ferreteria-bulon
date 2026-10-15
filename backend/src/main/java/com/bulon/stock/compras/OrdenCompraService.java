package com.bulon.stock.compras;

import com.bulon.stock.common.exception.BusinessException;
import com.bulon.stock.common.exception.ResourceNotFoundException;
import com.bulon.stock.movimientos.MovimientoRequest;
import com.bulon.stock.movimientos.MovimientoStockService;
import com.bulon.stock.movimientos.TipoMovimiento;
import com.bulon.stock.productos.ProductoDTO;
import com.bulon.stock.productos.ProductoService;
import com.bulon.stock.proveedores.ProveedorDTO;
import com.bulon.stock.proveedores.ProveedorService;
import com.bulon.stock.ubicaciones.UbicacionService;
import com.bulon.stock.usuarios.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrdenCompraService {

    private final OrdenCompraRepository ordenRepository;
    private final ProveedorService proveedorService;
    private final ProductoService productoService;
    private final UsuarioService usuarioService;
    private final UbicacionService ubicacionService;
    private final MovimientoStockService movimientoService;

    public Page<OrdenCompraDTO> listar(EstadoOrdenCompra estado, Pageable pageable) {
        return (estado != null ? ordenRepository.findByEstado(estado, pageable) : ordenRepository.findAllBy(pageable))
                .map(OrdenCompraDTO::from);
    }

    public OrdenCompraDTO obtener(Long id) {
        return OrdenCompraDTO.from(buscarEntidad(id));
    }

    @Transactional
    public OrdenCompraDTO crear(OrdenCompraRequest request, String username) {
        Set<Long> productos = new HashSet<>();
        OrdenCompra orden = new OrdenCompra();
        orden.setProveedor(proveedorService.buscarEntidad(request.proveedorId()));
        orden.setUsuario(usuarioService.buscarPorUsername(username));
        orden.setFechaCreacion(LocalDateTime.now());
        orden.setObservacion(request.observacion());
        for (OrdenCompraRequest.Item item : request.items()) {
            if (!productos.add(item.productoId())) {
                throw new BusinessException("El producto " + item.productoId() + " está repetido en la orden");
            }
            OrdenCompraItem linea = new OrdenCompraItem();
            linea.setOrden(orden);
            linea.setProducto(productoService.buscarEntidad(item.productoId()));
            linea.setCantidad(item.cantidad());
            orden.getItems().add(linea);
        }
        return OrdenCompraDTO.from(ordenRepository.save(orden));
    }

    @Transactional
    public OrdenCompraDTO enviar(Long id) {
        OrdenCompra orden = buscarEntidad(id);
        orden.pasarA(EstadoOrdenCompra.ENVIADA);
        return OrdenCompraDTO.from(orden);
    }

    @Transactional
    public OrdenCompraDTO cancelar(Long id) {
        OrdenCompra orden = buscarEntidad(id);
        orden.pasarA(EstadoOrdenCompra.CANCELADA);
        return OrdenCompraDTO.from(orden);
    }

    /** Registra un INGRESO_COMPRA por ítem con el usuario que recibe; todo o nada. */
    @Transactional
    public OrdenCompraDTO recibir(Long id, RecepcionRequest request, String username) {
        OrdenCompra orden = buscarEntidad(id);
        orden.pasarA(EstadoOrdenCompra.RECIBIDA);
        orden.setUbicacionRecepcion(ubicacionService.buscarEntidad(request.ubicacionId()));
        orden.setFechaRecepcion(LocalDateTime.now());
        for (OrdenCompraItem item : orden.getItems()) {
            movimientoService.registrar(new MovimientoRequest(item.getProducto().getId(), TipoMovimiento.INGRESO_COMPRA,
                    item.getCantidad(), request.ubicacionId(), null, null,
                    "Recepción de orden de compra #" + orden.getId()), username);
        }
        return OrdenCompraDTO.from(orden);
    }

    /** Sugerencia: reponer hasta el doble del punto de pedido (al menos una unidad). */
    public List<SugerenciaCompraDTO> sugerencias() {
        Map<Long, String> proveedores = proveedorService.listar().stream()
                .collect(Collectors.toMap(ProveedorDTO::id, ProveedorDTO::razonSocial));
        Map<Optional<Long>, List<ProductoDTO>> porProveedor = productoService.pendientesReposicion().stream()
                .collect(Collectors.groupingBy(p -> Optional.ofNullable(p.proveedorPreferidoId()),
                        LinkedHashMap::new, Collectors.toList()));
        return porProveedor.entrySet().stream()
                .map(e -> new SugerenciaCompraDTO(
                        e.getKey().orElse(null),
                        e.getKey().map(proveedores::get).orElse(null),
                        e.getValue().stream().map(p -> new SugerenciaCompraDTO.Item(p.id(), p.codigo(), p.nombre(),
                                p.estadoStock(), p.stockTotal(), p.puntoPedido(),
                                Math.max(1, 2 * p.puntoPedido() - p.stockTotal()))).toList()))
                .sorted(Comparator.comparing(SugerenciaCompraDTO::proveedor, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private OrdenCompra buscarEntidad(Long id) {
        return ordenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de compra", id));
    }
}
