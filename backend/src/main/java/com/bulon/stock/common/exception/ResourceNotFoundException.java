package com.bulon.stock.common.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String recurso, Object id) {
        super(recurso + " " + id + " no encontrado");
    }
}
