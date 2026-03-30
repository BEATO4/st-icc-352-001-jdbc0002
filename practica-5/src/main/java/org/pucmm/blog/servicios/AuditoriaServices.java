package org.pucmm.blog.servicios;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;

public class AuditoriaServices {

    // 1. Obtener la conexión usando la variable de entorno requerida
    private static Connection getConexion() throws Exception {
        String jdbcUrl = System.getenv("JDBC_DATABASE_URL");
        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            throw new Exception("La variable de entorno JDBC_DATABASE_URL no está configurada.");
        }
        return DriverManager.getConnection(jdbcUrl);
    }

    // 2. Crear la tabla en CockroachDB si no existe
    public static void crearTablaAuditoria() {
        String sql = "CREATE TABLE IF NOT EXISTS auditoria_login (" +
                "id SERIAL PRIMARY KEY, " +
                "usuario VARCHAR(255) NOT NULL, " +
                "fecha_hora TIMESTAMP NOT NULL" +
                ");";
        try (Connection con = getConexion(); Statement stmt = con.createStatement()) {
            stmt.execute(sql);
            System.out.println("Tabla de auditoría verificada en CockroachDB.");
        } catch (Exception e) {
            System.err.println("Advertencia: No se pudo verificar la tabla de auditoría (¿Configuraste JDBC_DATABASE_URL?): " + e.getMessage());
        }
    }

    // 3. Registrar el inicio de sesión exitoso
    public static void registrarLogin(String username) {
        String sql = "INSERT INTO auditoria_login (usuario, fecha_hora) VALUES (?, ?)";
        try (Connection con = getConexion(); PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setTimestamp(2, new Timestamp(new Date().getTime())); // Fecha y hora actual
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Error al registrar auditoría de login en la nube: " + e.getMessage());
        }
    }
}