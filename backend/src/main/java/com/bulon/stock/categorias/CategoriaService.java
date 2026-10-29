package com.bulon.stock.categorias;

import com.bulon.stock.common.exception.BusinessException;
import com.bulon.stock.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public List<Categoria> listar() {
        return categoriaRepository.findAll();
    }

    public Categoria obtener(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", id));
    }

    @Transactional
    public Categoria crear(Categoria categoria) {
        if (categoria.getNombre() == null || categoria.getNombre().isBlank()) {
            throw new BusinessException("El nombre de la categoría es obligatorio");
        }
        if (categoriaRepository.existsByNombreIgnoreCase(categoria.getNombre())) {
            throw new BusinessException("Ya existe la categoría " + categoria.getNombre());
        }
        return categoriaRepository.save(categoria);
    }
}
