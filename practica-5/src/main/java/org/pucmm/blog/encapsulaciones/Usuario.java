package org.pucmm.blog.encapsulaciones;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import java.io.Serializable;

@Entity
public class Usuario implements Serializable {

    @Id
    private String username;
    private String nombre;
    private String password;
    private boolean administrator;
    private boolean autor;

    @Lob
    private String fotoPerfilBase64; // Requerimiento #6: Foto en base64

    // Constructor vacío requerido por JPA
    public Usuario() {
    }

    public Usuario(String username, String nombre, String password, boolean administrator, boolean autor) {
        this.username = username;
        this.nombre = nombre;
        this.password = password;
        this.administrator = administrator;
        this.autor = autor;
    }

    // Getters y Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isAdministrator() { return administrator; }
    public void setAdministrator(boolean administrator) { this.administrator = administrator; }

    public boolean isAutor() { return autor; }
    public void setAutor(boolean autor) { this.autor = autor; }

    public String getFotoPerfilBase64() { return fotoPerfilBase64; }
    public void setFotoPerfilBase64(String fotoPerfilBase64) { this.fotoPerfilBase64 = fotoPerfilBase64; }
}