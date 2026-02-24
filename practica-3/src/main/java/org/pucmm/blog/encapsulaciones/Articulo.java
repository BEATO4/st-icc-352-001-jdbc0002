package org.pucmm.blog.encapsulaciones;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
public class Articulo implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String titulo;

    @Lob
    private String cuerpo;

    @ManyToOne
    private Usuario autor;

    @Temporal(TemporalType.TIMESTAMP)
    private Date fecha;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comentario> listaComentarios;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<Etiqueta> listaEtiquetas;

    // Constructor vacío requerido por JPA
    public Articulo() {
        this.listaComentarios = new ArrayList<>();
        this.listaEtiquetas = new ArrayList<>();
        this.fecha = new Date();
    }

    public Articulo(String titulo, String cuerpo, Usuario autor) {
        this.titulo = titulo;
        this.cuerpo = cuerpo;
        this.autor = autor;
        this.fecha = new Date();
        this.listaComentarios = new ArrayList<>();
        this.listaEtiquetas = new ArrayList<>();
    }

    public String getResumen() {
        if (cuerpo != null && cuerpo.length() > 70) {
            return cuerpo.substring(0, 70) + "...";
        }
        return cuerpo;
    }

    // Getters y Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getCuerpo() { return cuerpo; }
    public void setCuerpo(String cuerpo) { this.cuerpo = cuerpo; }

    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }

    public Usuario getAutor() { return autor; }
    public void setAutor(Usuario autor) { this.autor = autor; }

    public List<Comentario> getListaComentarios() { return listaComentarios; }
    public void setListaComentarios(List<Comentario> listaComentarios) { this.listaComentarios = listaComentarios; }

    public List<Etiqueta> getListaEtiquetas() { return listaEtiquetas; }
    public void setListaEtiquetas(List<Etiqueta> listaEtiquetas) { this.listaEtiquetas = listaEtiquetas; }
}