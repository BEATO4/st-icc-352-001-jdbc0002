package org.pucmm.eventos.models;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
public class Usuario implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    private String nombre;
    private String password;

    @Enumerated(EnumType.STRING)
    private Rol rol; //

    // Constructores, Getters y Setters
    public Usuario() {}

    public enum Rol {
        ADMINISTRADOR, ORGANIZADOR, PARTICIPANTE
    }
}