package org.pucmm.eventos.config;

import com.pucmm.events.enums.Role;
import com.pucmm.events.models.User;
import com.pucmm.events.utils.PasswordUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Crea el usuario administrador inicial si no existe.
 * Este usuario está marcado con systemUser=true y no puede ser eliminado.
 */
public class DatabaseSeeder {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private DatabaseSeeder() {}

    public static void seed() {
        String adminUsername = AppConfig.getAdminUsername();

        try (Session session = DatabaseConfig.getSessionFactory().openSession()) {
            // Verifica si ya existe el admin
            User existing = session.createQuery(
                            "FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", adminUsername)
                    .uniqueResult();

            if (existing != null) {
                log.info("Usuario administrador '{}' ya existe. Seed omitido.", adminUsername);
                return;
            }

            // Crea el admin inicial
            Transaction tx = session.beginTransaction();
            try {
                User admin = new User(
                        adminUsername,
                        "Administrador del Sistema",
                        AppConfig.getAdminEmail(),
                        PasswordUtil.hash(AppConfig.getAdminPassword()),
                        Role.ADMIN
                );
                admin.setSystemUser(true);

                session.persist(admin);
                tx.commit();

                log.info("✅ Usuario administrador creado: '{}' / '{}'",
                        adminUsername, AppConfig.getAdminEmail());
                log.warn("⚠️  Cambia la contraseña del administrador en producción.");

            } catch (Exception e) {
                tx.rollback();
                log.error("Error al crear el administrador inicial: {}", e.getMessage(), e);
            }
        }
    }
}