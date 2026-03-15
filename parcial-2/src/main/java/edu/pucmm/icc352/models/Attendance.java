package edu.pucmm.icc352.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // OneToOne garantiza: 1 Registration = máximo 1 Attendance
    // unique=true en la FK hace que la BD rechace duplicados a nivel físico
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "registration_id", nullable = false, unique = true)
    private Registration registration;

    // Hora exacta en que se escaneó el QR
    @Column(nullable = false, updatable = false)
    private LocalDateTime scannedAt = LocalDateTime.now();

    // ── Constructores ──────────────────────────────────────────
    public Attendance() {}

    public Attendance(Registration registration) {
        this.registration = registration;
    }

    // ── Getters ────────────────────────────────────────────────
    public Long getId()                     { return id; }
    public Registration getRegistration()   { return registration; }
    public LocalDateTime getScannedAt()     { return scannedAt; }
}