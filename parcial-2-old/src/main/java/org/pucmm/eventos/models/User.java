package org.pucmm.eventos.models;

import org.pucmm.eventos.enums.Role;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean blocked = false;

    /**
     * El usuario administrador inicial no puede eliminarse.
     * Se marca con este flag al ser creado por el seed.
     */
    @Column(nullable = false)
    private boolean systemUser = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    // Un organizador puede tener muchos eventos creados
    @OneToMany(mappedBy = "organizer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Event> organizedEvents = new ArrayList<>();

    // Un participante puede tener muchas inscripciones
    @OneToMany(mappedBy = "participant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Registration> registrations = new ArrayList<>();

    // ─── Lifecycle ────────────────────────────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt  = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ─── Constructors ─────────────────────────────────────────────────────────
    public User() {}

    public User(String username, String fullName, String email, String passwordHash, Role role) {
        this.username     = username;
        this.fullName     = fullName;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.role         = role;
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────
    public Long getId()                          { return id; }
    public void setId(Long id)                   { this.id = id; }

    public String getUsername()                  { return username; }
    public void setUsername(String username)     { this.username = username; }

    public String getFullName()                  { return fullName; }
    public void setFullName(String fullName)     { this.fullName = fullName; }

    public String getEmail()                     { return email; }
    public void setEmail(String email)           { this.email = email; }

    public String getPasswordHash()              { return passwordHash; }
    public void setPasswordHash(String hash)     { this.passwordHash = hash; }

    public Role getRole()                        { return role; }
    public void setRole(Role role)               { this.role = role; }

    public boolean isBlocked()                   { return blocked; }
    public void setBlocked(boolean blocked)      { this.blocked = blocked; }

    public boolean isSystemUser()                { return systemUser; }
    public void setSystemUser(boolean system)    { this.systemUser = system; }

    public LocalDateTime getCreatedAt()          { return createdAt; }
    public LocalDateTime getUpdatedAt()          { return updatedAt; }

    public List<Event> getOrganizedEvents()      { return organizedEvents; }
    public List<Registration> getRegistrations() { return registrations; }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    public boolean isAdmin()       { return Role.ADMIN.equals(this.role); }
    public boolean isOrganizer()   { return Role.ORGANIZER.equals(this.role); }
    public boolean isParticipant() { return Role.PARTICIPANT.equals(this.role); }
    public boolean canManageEvents() { return isAdmin() || isOrganizer(); }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', role=" + role + "}";
    }
}