package com.bulon.stock.common.auditoria;

import com.bulon.stock.auth.TokenService;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;

/**
 * Auditoría liviana de las tablas maestras: quién y cuándo las modificó por última vez.
 * Se completa sola al guardar: la fecha siempre y el usuario con el del token del request.
 * Fuera de un request (el seeder, la revisión de stock al arrancar) el usuario queda como estaba.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class EntidadAuditada {

    /** Id del último usuario que modificó la fila (FK a usuarios). */
    @Column(name = "actualizado_por")
    private Long actualizadoPor;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @PrePersist
    @PreUpdate
    protected void marcarActualizacion() {
        actualizadoEn = LocalDateTime.now();
        Long usuario = usuarioDelRequest();
        if (usuario != null) {
            actualizadoPor = usuario;
        }
    }

    private static Long usuarioDelRequest() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null && autenticacion.getPrincipal() instanceof Jwt jwt ? TokenService.usuarioId(jwt) : null;
    }
}
