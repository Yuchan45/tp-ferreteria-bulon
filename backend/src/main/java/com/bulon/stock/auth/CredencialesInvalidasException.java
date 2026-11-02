package com.bulon.stock.auth;

/** Login fallido: se responde 401 sin indicar si falló el usuario o la contraseña. */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException(String message) {
        super(message);
    }
}
