package org.pucmm.eventos.config;

/**
 * Centraliza la configuración de la aplicación leída desde variables de entorno.
 */
public class AppConfig {

    private AppConfig() {}

    /** Puerto en que escucha Javalin */
    public static int getPort() {
        return Integer.parseInt(System.getenv().getOrDefault("APP_PORT", "8080"));
    }

    /** Secreto para firmar sesiones / JWT si se implementa más adelante */
    public static String getSessionSecret() {
        return System.getenv().getOrDefault("SESSION_SECRET", "dev-secret-change-in-prod");
    }

    /** Credenciales del admin inicial */
    public static String getAdminUsername() {
        return System.getenv().getOrDefault("ADMIN_USERNAME", "admin");
    }

    public static String getAdminEmail() {
        return System.getenv().getOrDefault("ADMIN_EMAIL", "admin@pucmm.edu.do");
    }

    public static String getAdminPassword() {
        return System.getenv().getOrDefault("ADMIN_PASSWORD", "Admin1234!");
    }

    /** Modo desarrollo: muestra stack traces en respuestas de error */
    public static boolean isDevMode() {
        return "true".equalsIgnoreCase(System.getenv().getOrDefault("DEV_MODE", "true"));
    }
}