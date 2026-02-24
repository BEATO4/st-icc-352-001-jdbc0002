package org.pucmm.blog.servicios;

import org.pucmm.blog.encapsulaciones.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

public class UsuarioServices extends GestionDb<Usuario> {

    private static UsuarioServices instancia;

    private UsuarioServices() {
        super(Usuario.class);
    }

    public static UsuarioServices getInstancia() {
        if (instancia == null) {
            instancia = new UsuarioServices();
        }
        return instancia;
    }

    public Usuario autenticarUsuario(String username, String password) {
        EntityManager em = getEntityManager();
        try {
            // Consulta JPA (JPQL)
            return em.createQuery("select u from Usuario u where u.username = :username and u.password = :password", Usuario.class)
                    .setParameter("username", username)
                    .setParameter("password", password)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null; // Si no encuentra el usuario, retorna null
        } finally {
            em.close();
        }
    }
}