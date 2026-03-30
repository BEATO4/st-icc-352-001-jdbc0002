package org.pucmm.blog.servicios;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import org.pucmm.blog.encapsulaciones.ChatSesion;

import java.util.List;

public class ChatSesionServices extends GestionDb<ChatSesion> {

    private static ChatSesionServices instancia;

    private ChatSesionServices() {
        super(ChatSesion.class);
    }

    public static ChatSesionServices getInstancia() {
        if (instancia == null) {
            instancia = new ChatSesionServices();
        }
        return instancia;
    }

    public ChatSesion buscarPorToken(String token) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("select c from ChatSesion c where c.token = :token", ChatSesion.class)
                    .setParameter("token", token)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    public List<ChatSesion> listarAbiertas() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("select c from ChatSesion c where c.abierta = true order by c.fechaUltimoMensaje desc", ChatSesion.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<ChatSesion> listarRecientes() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("select c from ChatSesion c order by c.fechaUltimoMensaje desc", ChatSesion.class)
                    .setMaxResults(100)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<ChatSesion> listarPorPagina(String paginaOrigen) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("select c from ChatSesion c where c.paginaOrigen = :pagina order by c.fechaUltimoMensaje desc", ChatSesion.class)
                    .setParameter("pagina", paginaOrigen)
                    .setMaxResults(50)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public ChatSesion buscarPorNombreYPagina(String nombre, String pagina) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("select c from ChatSesion c where c.nombreChat = :nombre and c.paginaOrigen = :pagina and c.abierta = true", ChatSesion.class)
                    .setParameter("nombre", nombre)
                    .setParameter("pagina", pagina)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }
}

