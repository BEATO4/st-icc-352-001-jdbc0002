package org.pucmm.blog.encapsulaciones;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
public class ChatSesion implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true, nullable = false, length = 100)
    private String token;

    @Column(nullable = false, length = 120)
    private String nombreChat;

    @Column(nullable = true, length = 80)
    private String nombreInvitado;

    @Column(nullable = false, length = 120)
    private String paginaOrigen;

    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaCreacion;

    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaUltimoMensaje;

    private boolean abierta;

    private boolean esperandoAgente;

    @ManyToOne
    private Usuario creadoPor;

    @ManyToOne
    private Usuario atendidoPor;

    @OneToMany(mappedBy = "sesion", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fecha asc")
    private List<ChatMensaje> mensajes;

    public ChatSesion() {
        this.fechaCreacion = new Date();
        this.fechaUltimoMensaje = new Date();
        this.abierta = true;
        this.esperandoAgente = true;
        this.mensajes = new ArrayList<>();
    }

    public ChatSesion(String token, String nombreChat, String nombreInvitado, String paginaOrigen) {
        this();
        this.token = token;
        this.nombreChat = nombreChat;
        this.nombreInvitado = nombreInvitado;
        this.paginaOrigen = paginaOrigen;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getNombreChat() { return nombreChat; }
    public void setNombreChat(String nombreChat) { this.nombreChat = nombreChat; }

    public String getNombreInvitado() { return nombreInvitado; }
    public void setNombreInvitado(String nombreInvitado) { this.nombreInvitado = nombreInvitado; }

    public String getPaginaOrigen() { return paginaOrigen; }
    public void setPaginaOrigen(String paginaOrigen) { this.paginaOrigen = paginaOrigen; }

    public Date getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Date fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public Date getFechaUltimoMensaje() { return fechaUltimoMensaje; }
    public void setFechaUltimoMensaje(Date fechaUltimoMensaje) { this.fechaUltimoMensaje = fechaUltimoMensaje; }

    public boolean isAbierta() { return abierta; }
    public void setAbierta(boolean abierta) { this.abierta = abierta; }

    public boolean isEsperandoAgente() { return esperandoAgente; }
    public void setEsperandoAgente(boolean esperandoAgente) { this.esperandoAgente = esperandoAgente; }

    public Usuario getCreadoPor() { return creadoPor; }
    public void setCreadoPor(Usuario creadoPor) { this.creadoPor = creadoPor; }

    public Usuario getAtendidoPor() { return atendidoPor; }
    public void setAtendidoPor(Usuario atendidoPor) { this.atendidoPor = atendidoPor; }

    public List<ChatMensaje> getMensajes() { return mensajes; }
    public void setMensajes(List<ChatMensaje> mensajes) { this.mensajes = mensajes; }
}
