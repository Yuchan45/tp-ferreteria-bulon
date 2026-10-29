package com.bulon.stock.productos;

public enum UnidadMedida {
    UNIDAD(false),
    METRO(true),
    KILOGRAMO(true),
    LITRO(true),
    CAJA(false),
    ROLLO(false);

    /** Si se puede vender una fracción (1,5 m de cable) o solo enteros (2 cajas). */
    private final boolean fraccionable;

    UnidadMedida(boolean fraccionable) {
        this.fraccionable = fraccionable;
    }

    public boolean isFraccionable() {
        return fraccionable;
    }
}
