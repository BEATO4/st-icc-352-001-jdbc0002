package org.pucmm.eventos.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"usuario_id", "evento_id"})}) // [cite: 61]
public class Inscripcion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Usuario usuario;

    @ManyToOne
    private Evento evento;

    private String tokenValidacion; // [cite: 67]
    private LocalDateTime fechaInscripcion;
    private boolean asistio = false; // [cite: 75]
    private LocalDateTime fechaAsistencia;

    // Getters y Setters
}