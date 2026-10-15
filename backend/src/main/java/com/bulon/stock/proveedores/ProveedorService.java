package com.bulon.stock.proveedores;

import com.bulon.stock.common.exception.BusinessException;
import com.bulon.stock.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    public List<ProveedorDTO> listar() {
        return proveedorRepository.findAll(Sort.by("razonSocial")).stream().map(ProveedorDTO::from).toList();
    }

    public ProveedorDTO obtener(Long id) {
        return ProveedorDTO.from(buscarEntidad(id));
    }

    public Proveedor buscarEntidad(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor", id));
    }

    @Transactional
    public ProveedorDTO crear(ProveedorRequest request) {
        Proveedor proveedor = new Proveedor();
        aplicar(request, proveedor);
        return ProveedorDTO.from(proveedorRepository.save(proveedor));
    }

    @Transactional
    public ProveedorDTO actualizar(Long id, ProveedorRequest request) {
        Proveedor proveedor = buscarEntidad(id);
        aplicar(request, proveedor);
        return ProveedorDTO.from(proveedor);
    }

    /** Falla con 409 si el proveedor está asociado a productos u órdenes de compra. */
    @Transactional
    public void eliminar(Long id) {
        proveedorRepository.delete(buscarEntidad(id));
        proveedorRepository.flush();
    }

    private void aplicar(ProveedorRequest request, Proveedor proveedor) {
        String cuit = request.cuit() == null || request.cuit().isBlank() ? null : request.cuit();
        if (cuit != null) {
            proveedorRepository.findByCuit(cuit)
                    .filter(p -> !p.getId().equals(proveedor.getId()))
                    .ifPresent(p -> {
                        throw new BusinessException("Ya existe un proveedor con CUIT " + cuit);
                    });
        }
        proveedor.setRazonSocial(request.razonSocial().trim());
        proveedor.setCuit(cuit);
        proveedor.setTelefono(request.telefono());
        proveedor.setEmail(request.email());
    }
}
