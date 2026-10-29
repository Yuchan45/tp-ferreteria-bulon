package com.bulon.stock.sucursales;

import com.bulon.stock.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SucursalService {

    private final SucursalRepository sucursalRepository;

    public List<Sucursal> listar() {
        return sucursalRepository.findAll();
    }

    public Sucursal obtener(Long id) {
        return sucursalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal", id));
    }

    public Optional<Sucursal> buscarPorCodigo(String codigo) {
        return sucursalRepository.findByCodigo(codigo);
    }

    @Transactional
    public Sucursal crear(Sucursal sucursal) {
        return sucursalRepository.save(sucursal);
    }
}
