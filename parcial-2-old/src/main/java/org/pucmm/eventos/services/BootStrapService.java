package org.pucmm.eventos.services;

import org.pucmm.eventos.models.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class BootStrapService {
    private static EntityManagerFactory emf;

    public static void init() {
        emf = Persistence.createEntityManagerFactory("EventosPU");
        crearAdminPorDefecto();
    }

    private static void crearAdminPorDefecto() {
        EntityManager em = emf.createEntityManager();
        try {
            long count = (long) em.createQuery("SELECT count(u) FROM Usuario u WHERE u.rol = :rol")
                    .setParameter("rol", Usuario.Rol.ADMINISTRADOR)
                    .getSingleResult();

            if (count == 0) { //
                em.getTransaction().begin();
                Usuario admin = new Usuario();
                admin.setUsername("admin");
                admin.setPassword("admin123"); // Deberías usar BCrypt
                admin.setRol(Usuario.Rol.ADMINISTRADOR);
                em.persist(admin);
                em.getTransaction().commit();
                System.out.println("Usuario Administrador inicial creado.");
            }
        } finally {
            em.close();
        }
    }

    public static EntityManager getEntityManager() {
        return emf.createEntityManager();
    }
}