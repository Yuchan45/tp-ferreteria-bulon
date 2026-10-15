package com.bulon.stock.ubicaciones;

import com.bulon.stock.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UbicacionService {

    private final UbicacionRepository ubicacionRepository;

    public List<UbicacionDTO> listar() {
        return ubicacionRepository.findAll(Sort.by("id")).stream().map(UbicacionDTO::from).toList();
    }

    public Ubicacion buscarEntidad(Long id) {
        return ubicacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ubicación", id));
    }

    public Ubicacion buscarPorNombre(String nombre) {
        return ubicacionRepository.findByNombre(nombre)
                .orElseThrow(() -> new ResourceNotFoundException("Ubicación", nombre));
    }
}
