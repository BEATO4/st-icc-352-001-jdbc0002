package edu.pucmm.icc352.utils;

import edu.pucmm.icc352.config.HibernateConfig;
import edu.pucmm.icc352.models.Role;
import edu.pucmm.icc352.models.User;
import org.hibernate.Session;
import org.mindrot.jbcrypt.BCrypt;

public class SeedData {

    public static void createDefaultAdmin() {
        try (Session session = HibernateConfig.getSessionFactory().openSession()) {

            // Verificar si ya existe el admin
            User existing = session.createQuery(
                            "FROM User WHERE email = :email", User.class)
                    .setParameter("email", "admin@eventos.com")
                    .uniqueResult();

            if (existing == null) {
                var tx = session.beginTransaction();

                User admin = new User(
                        "admin@eventos.com",
                        BCrypt.hashpw("Admin1234!", BCrypt.gensalt()),
                        "Administrador",
                        Role.ADMIN
                );
                admin.setDeletable(false);  // no puede ser eliminado

                session.persist(admin);
                tx.commit();

                System.out.println("Admin creado → admin@eventos.com / Admin1234!");
            } else {
                System.out.println("Admin ya existe, saltando seed.");
            }
        }
    }
}