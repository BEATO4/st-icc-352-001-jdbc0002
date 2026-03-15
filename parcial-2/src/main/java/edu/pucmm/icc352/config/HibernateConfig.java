package edu.pucmm.icc352.config;

import org.h2.tools.Server;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateConfig {

    private static SessionFactory sessionFactory;
    private static Server h2Server;

    public static void init() {
        String dbHost = System.getenv("DB_HOST");

        if (dbHost == null || dbHost.isEmpty()) {
            startEmbeddedH2();
        } else {
            System.out.println("Modo Docker: conectando a H2 en " + dbHost);
        }

        String host = dbHost != null ? dbHost : "localhost";
        String port = System.getenv("DB_PORT") != null ? System.getenv("DB_PORT") : "9092";
        String name = System.getenv("DB_NAME") != null ? System.getenv("DB_NAME") : "eventosdb";
        String url  = "jdbc:h2:tcp://" + host + ":" + port + "/~/" + name + ";IFEXISTS=FALSE";

        // Retry hasta que H2 esté listo
        int attempts = 0;
        while (attempts < 10) {
            try {
                sessionFactory = new Configuration()
                        .configure("hibernate.cfg.xml")
                        .setProperty("hibernate.connection.url", url)
                        .buildSessionFactory();
                System.out.println("Hibernate conectado: " + url);
                return;
            } catch (Exception e) {
                attempts++;
                System.out.println("H2 no listo, reintento " + attempts + "/10...");
                try { Thread.sleep(3000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
        throw new RuntimeException("No se pudo conectar a H2 después de 10 intentos");
    }

    private static void startEmbeddedH2() {
        try {
            h2Server = Server.createTcpServer(
                    "-tcp",
                    "-tcpAllowOthers",
                    "-tcpPort", "9092",
                    "-ifNotExists"
            ).start();
            System.out.println("H2 Server arrancado en puerto 9092");
        } catch (Exception e) {
            throw new RuntimeException("No se pudo iniciar H2: " + e.getMessage());
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        if (sessionFactory != null) sessionFactory.close();
        if (h2Server != null) h2Server.stop();
    }
}