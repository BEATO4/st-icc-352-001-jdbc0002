package org.pucmm.eventos.config;

import org.h2.tools.Server;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.pucmm.eventos.model.Event;
import org.pucmm.eventos.model.Registration;
import org.pucmm.eventos.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.SQLException;

public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);
    private static SessionFactory sessionFactory;
    private static Server h2Server;

    public static void init() {
        try {
            String dataDir = System.getenv().getOrDefault("H2_DATA_DIR", "./data");
            new File(dataDir).mkdirs();

            // Start H2 in TCP server mode
            h2Server = Server.createTcpServer(
                    "-tcp", "-tcpAllowOthers",
                    "-tcpPort", "9092",
                    "-baseDir", dataDir,
                    "-ifNotExists"
            ).start();
            log.info("H2 TCP server started on port 9092, data dir: {}", dataDir);

            String jdbcUrl = "jdbc:h2:tcp://localhost:9092/eventosdb;DB_CLOSE_DELAY=-1;MODE=MySQL";

            StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                    .applySetting("hibernate.connection.url", jdbcUrl)
                    .applySetting("hibernate.connection.username", "sa")
                    .applySetting("hibernate.connection.password", "")
                    .applySetting("hibernate.connection.driver_class", "org.h2.Driver")
                    .applySetting("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
                    .applySetting("hibernate.hbm2ddl.auto", "update")
                    .applySetting("hibernate.show_sql", "false")
                    .applySetting("hibernate.format_sql", "false")
                    .applySetting("hibernate.connection.pool_size", "10")
                    .build();

            MetadataSources sources = new MetadataSources(registry);
            sources.addAnnotatedClass(User.class);
            sources.addAnnotatedClass(Event.class);
            sources.addAnnotatedClass(Registration.class);

            sessionFactory = sources.buildMetadata().buildSessionFactory();
            log.info("Hibernate SessionFactory initialised");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to start H2 TCP server", e);
        }
    }

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) throw new IllegalStateException("DatabaseConfig not initialised");
        return sessionFactory;
    }

    public static void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) sessionFactory.close();
        if (h2Server != null) h2Server.stop();
        log.info("Database shutdown complete");
    }
}