package com.bulon.stock.proveedores;

import com.bulon.stock.common.auditoria.EntidadAuditada;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Empresa externa a la que se compra. No se gestiona su stock. */
// Si Hibernate la devuelve como proxy (carga diferida), Jackson ignora sus campos internos
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "proveedores")
@Getter
@Setter
@NoArgsConstructor
public class Proveedor extends EntidadAuditada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String razonSocial;

    @Column(unique = true, length = 13)
    private String cuit;

    @Column(length = 30)
    private String telefono;

    @Column(length = 150)
    private String email;

    /** Persona de contacto. */
    @Column(length = 100)
    private String contacto;

    @Column(nullable = false)
    private boolean activo = true;
}
