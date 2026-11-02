package com.bulon.stock.proveedores;

import com.bulon.stock.common.exception.BusinessException;
import com.bulon.stock.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    public List<Proveedor> listar() {
        return proveedorRepository.findAll();
    }

    public Proveedor obtener(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor", id));
    }

    @Transactional
    public Proveedor crear(Proveedor proveedor) {
        return proveedorRepository.save(proveedor);
    }

    /**
     * Edita los datos de contacto y el estado. Un proveedor desactivado sigue en el historial
     * (compras y devoluciones viejas) pero ya no se le pueden registrar compras.
     */
    @Transactional
    public Proveedor actualizar(Long id, Proveedor datos) {
        if (datos.getRazonSocial() == null || datos.getRazonSocial().isBlank()) {
            throw new BusinessException("La razón social es obligatoria");
        }
        Proveedor proveedor = obtener(id);
        proveedor.setRazonSocial(datos.getRazonSocial().trim());
        proveedor.setCuit(blancoANull(datos.getCuit()));
        proveedor.setTelefono(blancoANull(datos.getTelefono()));
        proveedor.setEmail(blancoANull(datos.getEmail()));
        proveedor.setContacto(blancoANull(datos.getContacto()));
        proveedor.setActivo(datos.isActivo());
        // Un CUIT repetido lo rechaza el índice único al guardar: 409 Conflict
        return proveedorRepository.saveAndFlush(proveedor);
    }

    private static String blancoANull(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
