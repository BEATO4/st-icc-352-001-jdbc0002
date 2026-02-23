package org.pucmm.blog;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
import org.pucmm.blog.encapsulaciones.*;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.rendering.template.JavalinThymeleaf; //motor de plantillas
import java.util.*;

import org.h2.tools.Server;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.sql.SQLException;

public class Main {

    public static EntityManagerFactory emf;

    // 1. Persistencia en Memoria
    private static List<Usuario> usuarios = new ArrayList<>();
    private static List<Articulo> articulos = new ArrayList<>();
    private static long articuloIdCounter = 1;
    private static long comentarioIdCounter = 1;

    public static void main(String[] args) throws SQLException {

        // 2. Crear usuario admin por defecto al iniciar
        usuarios.add(new Usuario("admin", "Administrador", "admin", true, true));
        // Usuario  prueba autor
        usuarios.add(new Usuario("autor", "Autor Demo", "1234", false, true));

        // Datos para probar la vista
        Articulo a1 = new Articulo("Primer Post", "Este es el cuerpo del artículo que debe ser largo para probar el resumen de 70 caracteres que pide la práctica.", usuarios.get(0));
        a1.getListaEtiquetas().add(new Etiqueta("Tecnologia"));
        articulos.add(a1);

        // 1. Iniciar el servidor de H2 de forma automática en el puerto 9092
        Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9092").start();
        System.out.println("Servidor H2 iniciado en modo TCP.");

        // 2. Inicializar JPA usando la unidad de persistencia configurada en persistence.xml
        emf = Persistence.createEntityManagerFactory("BlogPU");
        System.out.println("Conexión a la base de datos establecida y tablas creadas.");

        Javalin app = Javalin.create(config -> {
            // Configurar archivos
            config.staticFiles.add("/public", Location.CLASSPATH);
            config.fileRenderer(new JavalinThymeleaf());
        }).start(7000);

        //FILTROS DE SEGURIDAD
        app.before("/admin/*", ctx -> {
            Usuario usuario = ctx.sessionAttribute("usuario");
            if (usuario == null) {
                ctx.redirect("/login");
            }
        });

        app.before("/crear-articulo", ctx -> {
            Usuario usuario = ctx.sessionAttribute("usuario");
            // Solo admin o autor pueden crear [cite: 61, 69]
            if (usuario == null || (!usuario.isAdministrator() && !usuario.isAutor())) {
                ctx.redirect("/login");
            }
        });

        //RUTAS
        app.get("/", ctx -> {
            Map<String, Object> modelo = new HashMap<>();

            articulos.sort((a, b) -> b.getFecha().compareTo(a.getFecha()));

            modelo.put("articulos", articulos);
            modelo.put("usuario", ctx.sessionAttribute("usuario"));

            ctx.render("/templates/index.html", modelo);
        });

        app.get("/login", ctx -> ctx.render("/templates/login.html"));

    }
}