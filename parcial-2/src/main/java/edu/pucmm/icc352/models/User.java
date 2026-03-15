package edu.pucmm.icc352.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;  // siempre guardado como hash BCrypt

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.PARTICIPANT;  // rol por defecto al registrarse

    @Column(nullable = false)
    private boolean blocked = false;

    // false = este usuario NO puede ser eliminado (solo el admin por defecto)
    @Column(nullable = false)
    private boolean deletable = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ── Constructores ──────────────────────────────────────────
    public User() {}

    public User(String email, String password, String name, Role role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
    }

    // ── Getters y Setters ──────────────────────────────────────
    public Long getId()                    { return id; }
    public String getEmail()               { return email; }
    public void setEmail(String email)     { this.email = email; }
    public String getPassword()            { return password; }
    public void setPassword(String p)      { this.password = p; }
    public String getName()                { return name; }
    public void setName(String name)       { this.name = name; }
    public Role getRole()                  { return role; }
    public void setRole(Role role)         { this.role = role; }
    public boolean isBlocked()             { return blocked; }
    public void setBlocked(boolean b)      { this.blocked = b; }
    public boolean isDeletable()           { return deletable; }
    public void setDeletable(boolean d)    { this.deletable = d; }
    public LocalDateTime getCreatedAt()    { return createdAt; }
}