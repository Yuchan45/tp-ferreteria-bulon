package com.bulon.stock.common.auditoria;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Auditoría liviana de las tablas maestras: quién y cuándo las modificó por última vez.
 * La fecha se completa sola; el usuario queda en null hasta que exista el login.
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
    }
}
