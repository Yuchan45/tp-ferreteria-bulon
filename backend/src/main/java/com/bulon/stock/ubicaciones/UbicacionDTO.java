package com.bulon.stock.ubicaciones;

public record UbicacionDTO(Long id, String nombre) {

    public static UbicacionDTO from(Ubicacion u) {
        return new UbicacionDTO(u.getId(), u.getNombre());
    }
}
