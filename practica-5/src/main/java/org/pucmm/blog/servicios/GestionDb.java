package org.pucmm.blog.servicios;

import jakarta.persistence.EntityManager;
import org.pucmm.blog.Main;

import java.util.List;

public class GestionDb<T> {

    private Class<T> claseEntidad;

    public GestionDb(Class<T> claseEntidad) {
        this.claseEntidad = claseEntidad;
    }

    public EntityManager getEntityManager() {
        // Obtenemos el EntityManagerFactory que inicializamos en el Main
        return Main.emf.createEntityManager();
    }

    public T crear(T entidad) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(entidad);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
        return entidad;
    }

    public T editar(T entidad) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(entidad);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
        return entidad;
    }

    public boolean eliminar(Object id) {
        boolean ok = false;
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            T entidad = em.find(claseEntidad, id);
            if (entidad != null) {
                em.remove(entidad);
                ok = true;
            }
            em.getTransaction().commit();
        } finally {
            em.close();
        }
        return ok;
    }

    public T buscar(Object id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(claseEntidad, id);
        } finally {
            em.close();
        }
    }

    public List<T> buscarTodos() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("select e from " + claseEntidad.getSimpleName() + " e", claseEntidad).getResultList();
        } finally {
            em.close();
        }
    }
}