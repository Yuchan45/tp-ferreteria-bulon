package com.bulon.stock.productos;

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
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public List<CategoriaDTO> listar() {
        return categoriaRepository.findAll(Sort.by("nombre")).stream().map(CategoriaDTO::from).toList();
    }

    public Categoria buscarEntidad(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", id));
    }

    @Transactional
    public CategoriaDTO crear(CategoriaDTO dto) {
        String nombre = dto.nombre().trim();
        categoriaRepository.findByNombreIgnoreCase(nombre).ifPresent(c -> {
            throw new BusinessException("Ya existe la categoría " + c.getNombre());
        });
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        return CategoriaDTO.from(categoriaRepository.save(categoria));
    }
}
