package com.bulon.stock.notificaciones.canales;

import com.bulon.stock.alertas.AlertaStock;
import com.bulon.stock.usuarios.Usuario;

/**
 * Patrón Decorator: interfaz común del notificador base y de sus decoradores. Quien
 * notifica no sabe por cuántos canales sale el aviso; solo llama a {@link #enviar}.
 */
public interface Notificador {

    void enviar(Usuario destinatario, AlertaStock alerta, String titulo, String mensaje);
}
