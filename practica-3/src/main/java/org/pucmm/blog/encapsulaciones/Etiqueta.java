package org.pucmm.blog.encapsulaciones;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.io.Serializable;

@Entity
public class Etiqueta implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String etiqueta;

    // Constructor vacío requerido por JPA
    public Etiqueta() {
    }

    public Etiqueta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    // Getters y Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getEtiqueta() { return etiqueta; }
    public void setEtiqueta(String etiqueta) { this.etiqueta = etiqueta; }
}