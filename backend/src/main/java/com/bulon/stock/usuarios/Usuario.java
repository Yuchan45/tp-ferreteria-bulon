package com.bulon.stock.usuarios;

import com.bulon.stock.sucursales.Sucursal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Persona que opera el sistema. Nunca se expone en la API: ver {@link UsuarioDTO}. */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(unique = true, length = 150)
    private String email;

    /** Hash BCrypt de la contraseña. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    /** Sucursal del empleado; null para el administrador, que ve todas. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime creado = LocalDateTime.now();

    /** El administrador opera todas las sucursales; el empleado, solo la suya. */
    public boolean puedeOperar(Sucursal destino) {
        return rol == Rol.ADMINISTRADOR || (sucursal != null && sucursal.getId().equals(destino.getId()));
    }
}
