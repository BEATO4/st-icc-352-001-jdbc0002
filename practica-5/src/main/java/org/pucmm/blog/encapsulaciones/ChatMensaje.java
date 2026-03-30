package org.pucmm.blog.encapsulaciones;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;
@Entity
public class ChatMensaje implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne(optional = false)
    private ChatSesion sesion;
    @Column(nullable = false, length = 80)
    private String emisorNombre;
    @Column(nullable = false, length = 20)
    private String emisorTipo;
    @Lob
    @Column(nullable = false)
    private String contenido;
    @Temporal(TemporalType.TIMESTAMP)
    private Date fecha;
    public ChatMensaje() {
        this.fecha = new Date();
    }
    public ChatMensaje(ChatSesion sesion, String emisorNombre, String emisorTipo, String contenido) {
        this();
        this.sesion = sesion;
        this.emisorNombre = emisorNombre;
        this.emisorTipo = emisorTipo;
        this.contenido = contenido;
    }
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public ChatSesion getSesion() { return sesion; }
    public void setSesion(ChatSesion sesion) { this.sesion = sesion; }
    public String getEmisorNombre() { return emisorNombre; }
    public void setEmisorNombre(String emisorNombre) { this.emisorNombre = emisorNombre; }
    public String getEmisorTipo() { return emisorTipo; }
    public void setEmisorTipo(String emisorTipo) { this.emisorTipo = emisorTipo; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }
}
