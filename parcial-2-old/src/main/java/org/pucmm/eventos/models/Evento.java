package org.pucmm.eventos.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Evento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    @Column(columnDefinition = "TEXT")
    private String descripcion;
    private LocalDateTime fechaHora;
    private String lugar;
    private int cupoMaximo;
    private boolean publicado = false; // [cite: 55]

    @ManyToOne
    private Usuario organizador;

    // Getters y Setters
}