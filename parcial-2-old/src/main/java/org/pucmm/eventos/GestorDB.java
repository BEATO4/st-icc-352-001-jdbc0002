package org.pucmm.eventos;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.pucmm.eventos.entidades.Rol;
import org.pucmm.eventos.entidades.Usuario;

public class GestorDB {

    private static SessionFactory sessionFactory;

    // Se ejecuta una sola vez para construir la fábrica de sesiones de Hibernate
    public static void inicializar() {
        if (sessionFactory == null) {
            try {
                sessionFactory = new Configuration().configure().buildSessionFactory();
                System.out.println("✅ Hibernate y H2 inicializados correctamente.");
                crearAdminPorDefecto();
            } catch (Exception e) {
                System.err.println("❌ Error iniciando Hibernate: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static Session getSession() {
        return sessionFactory.openSession();
    }

    // Cumple la rúbrica: Generar un admin si la base de datos está vacía
    private static void crearAdminPorDefecto() {
        try (Session session = getSession()) {
            // Buscamos si ya existe el usuario "admin"
            Usuario adminExistente = session.createQuery("FROM Usuario WHERE username = :user", Usuario.class)
                    .setParameter("user", "admin")
                    .uniqueResult();

            if (adminExistente == null) {
                Transaction tx = session.beginTransaction();
                // Nota: En un entorno real, la contraseña debe estar hasheada (ej. JBCrypt)
                Usuario nuevoAdmin = new Usuario("admin", "admin123", Rol.ADMINISTRADOR);
                session.persist(nuevoAdmin);
                tx.commit();
                System.out.println("👑 Usuario administrador por defecto creado exitosamente.");
            }
        }
    }
}