package com.bulon.stock.productos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaDTO(Long id, @NotBlank @Size(max = 80) String nombre) {

    public static CategoriaDTO from(Categoria c) {
        return new CategoriaDTO(c.getId(), c.getNombre());
    }
}
