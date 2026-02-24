package org.pucmm.blog;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
import org.pucmm.blog.encapsulaciones.*;
import org.pucmm.blog.servicios.ArticuloServices;
import org.pucmm.blog.servicios.UsuarioServices;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.rendering.template.JavalinThymeleaf;
import org.h2.tools.Server;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jasypt.util.text.AES256TextEncryptor;
import io.javalin.http.UploadedFile;
import java.util.Base64;

public class Main {

    // Instancia global del Factory de JPA
    public static EntityManagerFactory emf;

    public static void main(String[] args) throws SQLException {

        AES256TextEncryptor encriptador = new AES256TextEncryptor();
        encriptador.setPassword("ClaveSecreta_Blog_ICC352");

        Server.createTcpServer("-tcp", "-tcpAllowOthers", "-ifNotExists", "-tcpPort", "9092").start();
        System.out.println("Servidor H2 iniciado en modo TCP.");

        emf = Persistence.createEntityManagerFactory("BlogPU");
        System.out.println("Conexión a BD establecida.");


        org.pucmm.blog.servicios.AuditoriaServices.crearTablaAuditoria();

        // 3. Crear datos por defecto
        if (UsuarioServices.getInstancia().buscar("admin") == null) {
            Usuario admin = new Usuario("admin", "Administrador", "admin", true, true);
            UsuarioServices.getInstancia().crear(admin);

            Usuario autor = new Usuario("autor", "Autor Demo", "1234", false, true);
            UsuarioServices.getInstancia().crear(autor);

            Articulo a1 = new Articulo("Primer Post en H2", "Este es el cuerpo del artículo guardado en la base de datos H2 mediante JPA.", admin);
            a1.getListaEtiquetas().add(new Etiqueta("Tecnologia"));
            ArticuloServices.getInstancia().crear(a1);
        }

        // 4. Iniciar Javalin
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public", Location.CLASSPATH);
            config.fileRenderer(new JavalinThymeleaf());
        }).start(7000);

        app.before(ctx -> {
            Usuario usuarioSesion = ctx.sessionAttribute("usuario");
            if (usuarioSesion == null) {
                String cookieRecordar = ctx.cookie("usuario_recordado");
                if (cookieRecordar != null) {
                    try {
                        // Desencriptamos el username de la cookie
                        String usernameDecifrado = encriptador.decrypt(cookieRecordar);
                        Usuario usuarioBD = UsuarioServices.getInstancia().buscar(usernameDecifrado);
                        if (usuarioBD != null) {
                            ctx.sessionAttribute("usuario", usuarioBD); // Auto-login
                        }
                    } catch (Exception e) {
                        // Si la cookie fue alterada o la clave no coincide, la borramos
                        ctx.removeCookie("usuario_recordado");
                    }
                }
            }
        });

        // FILTROS DE SEGURIDAD
        app.before("/admin/*", ctx -> {
            Usuario usuario = ctx.sessionAttribute("usuario");
            if (usuario == null) {
                ctx.redirect("/login");
            }
        });

        app.before("/crear-articulo", ctx -> {
            Usuario usuario = ctx.sessionAttribute("usuario");
            if (usuario == null || (!usuario.isAdministrator() && !usuario.isAutor())) {
                ctx.redirect("/login");
            }
        });

        // RUTAS
        app.get("/", ctx -> {
            Map<String, Object> modelo = new HashMap<>();

            String etiqueta = ctx.queryParam("etiqueta");
            String paginaStr = ctx.queryParam("pagina");

            // 2. Configuramos la paginación
            int paginaActual = 1;
            if (paginaStr != null) {
                try { paginaActual = Integer.parseInt(paginaStr); }
                catch (NumberFormatException e) { paginaActual = 1; }
            }
            int articulosPorPagina = 5; // Requerimiento: Mostrar solo 5 artículos por página

            List<Articulo> articulosMostrados;
            long totalArticulos = 0;
            int totalPaginas = 1;

            if (etiqueta != null && !etiqueta.isEmpty()) {
                // Si el usuario hizo clic en una etiqueta, mostramos esos artículos
                articulosMostrados = ArticuloServices.getInstancia().buscarPorEtiqueta(etiqueta);
                modelo.put("etiquetaFiltro", etiqueta); // Para saber que estamos en modo filtro
            } else {
                // Si no hay etiqueta, mostramos la página normal (5 artículos)
                articulosMostrados = ArticuloServices.getInstancia().buscarArticulosPaginados(paginaActual, articulosPorPagina);
                totalArticulos = ArticuloServices.getInstancia().contarArticulos();
                // Calculamos el total de páginas redondeando hacia arriba
                totalPaginas = (int) Math.ceil((double) totalArticulos / articulosPorPagina);
            }

            // 4. Enviar a Thymeleaf
            modelo.put("articulos", articulosMostrados);
            modelo.put("usuario", ctx.sessionAttribute("usuario"));
            modelo.put("paginaActual", paginaActual);
            modelo.put("totalPaginas", Math.max(1, totalPaginas)); // Asegurar que mínimo haya 1 página

            ctx.render("/templates/index.html", modelo);
        });

        app.get("/login", ctx -> {
            Map<String, Object> modelo = new HashMap<>();
            String errorParam = ctx.queryParam("error");
            if (errorParam != null) {
                modelo.put("error", true);
            }
            ctx.render("/templates/login.html", modelo);
        });

        // Procesar el Login
        app.post("/login", ctx -> {
            String username = ctx.formParam("username");
            String pass = ctx.formParam("password");
            String recordar = ctx.formParam("recordar"); // Será "on" si el checkbox fue marcado

            Usuario authUser = UsuarioServices.getInstancia().autenticarUsuario(username, pass);

            if (authUser != null) {
                ctx.sessionAttribute("usuario", authUser);

                org.pucmm.blog.servicios.AuditoriaServices.registrarLogin(authUser.getUsername());

                if ("on".equals(recordar)) {
                    String usernameEncriptado = encriptador.encrypt(authUser.getUsername());
                    // Cookie dura 604800 segundos (1 semana)
                    ctx.cookie("usuario_recordado", usernameEncriptado, 604800);
                }
                ctx.redirect("/");
            } else {
                ctx.redirect("/login?error");  // Credenciales incorrectas
            }
        });

        // Procesar el Logout
        app.get("/logout", ctx -> {
            ctx.req().getSession().invalidate(); // Destruye la sesión
            ctx.removeCookie("usuario_recordado"); // Destruye la cookie de Jasypt
            ctx.redirect("/");
        });

        // Mostrar formulario de creación de usuario (solo admin)
        app.get("/admin/crear-usuario", ctx -> {
            ctx.render("/templates/crear_usuario.html", new HashMap<>());
        });

        // Procesar la creación de un nuevo usuario
        app.post("/crear-usuario", ctx -> {
            String username = ctx.formParam("username");
            String nombre = ctx.formParam("nombre");
            String pass = ctx.formParam("password");
            boolean esAutor = ctx.formParam("autor") != null;
            boolean esAdmin = ctx.formParam("admin") != null;

            Usuario nuevoUsuario = new Usuario(username, nombre, pass, esAdmin, esAutor);

            UploadedFile foto = ctx.uploadedFile("fotoPerfil");
            if (foto != null && foto.size() > 0) {
                try {
                    byte[] bytes = foto.content().readAllBytes();
                    String base64Image = Base64.getEncoder().encodeToString(bytes);
                    String mimeType = foto.contentType(); // Ej: image/jpeg o image/png
                    // Guardamos la cadena completa lista para usar en la etiqueta <img> de HTML
                    nuevoUsuario.setFotoPerfilBase64("data:" + mimeType + ";base64," + base64Image);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            UsuarioServices.getInstancia().crear(nuevoUsuario);
            ctx.redirect("/");
        });

        app.get("/crear-articulo", ctx -> {
            ctx.render("/templates/crear_articulo.html");
        });

        app.post("/crear-articulo", ctx -> {
            String titulo = ctx.formParam("titulo");
            String cuerpo = ctx.formParam("cuerpo");
            String etiquetasParam = ctx.formParam("etiquetas");
            Usuario autor = ctx.sessionAttribute("usuario");

            Articulo nuevoArticulo = new Articulo(titulo, cuerpo, autor);

            if (etiquetasParam != null && !etiquetasParam.trim().isEmpty()) {
                String[] tags = etiquetasParam.split(",");
                for (String tag : tags) {
                    nuevoArticulo.getListaEtiquetas().add(new Etiqueta(tag.trim()));
                }
            }

            ArticuloServices.getInstancia().crear(nuevoArticulo);
            ctx.redirect("/");
        });

        app.get("/articulo/{id}", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Articulo articulo = ArticuloServices.getInstancia().buscar(id);

                if (articulo == null) {
                    ctx.status(404).result("Artículo no encontrado");
                    return;
                }

                Map<String, Object> modelo = new HashMap<>();
                modelo.put("articulo", articulo);
                modelo.put("usuario", ctx.sessionAttribute("usuario"));

                ctx.render("/templates/articulo.html", modelo);
            } catch (NumberFormatException e) {
                ctx.status(400).result("ID de artículo inválido");
            }
        });

        app.post("/articulo/{id}/comentario", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                String textoComentario = ctx.formParam("comentario");
                Usuario autor = ctx.sessionAttribute("usuario");

                Articulo articulo = ArticuloServices.getInstancia().buscar(id);

                if (articulo != null && autor != null && textoComentario != null && !textoComentario.trim().isEmpty()) {
                    Comentario nuevoComentario = new Comentario(textoComentario, autor);
                    articulo.getListaComentarios().add(nuevoComentario);

                    ArticuloServices.getInstancia().editar(articulo);
                }

                ctx.redirect("/articulo/" + id);
            } catch (Exception e) {
                ctx.redirect("/");
            }
        });
    }
}