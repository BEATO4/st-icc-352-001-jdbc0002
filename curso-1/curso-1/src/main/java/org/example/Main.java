package org.ejemplo;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

public class Main {
    public static void main(String[] args) {
        // Inicializar Javalin y configurar la carpeta para archivos estáticos (CSS)
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public", Location.CLASSPATH);
        }).start(7070);

        // 1) Filtro para interceptar peticiones si no hay sesión
        app.before(ctx -> {
            String path = ctx.path();
            // Permitimos el acceso a la ruta de login y al archivo CSS para evitar un loop infinito
            if (!path.startsWith("/login") && !path.startsWith("/style.css")) {
                String usuario = ctx.sessionAttribute("usuario");
                if (usuario == null) {
                    ctx.redirect("/login");
                }
            }
        });

        // Ruta GET para mostrar el formulario de login
        app.get("/login", ctx -> {
            // Si el login falla, mostramos un mensaje
            String errorMsg = ctx.queryParam("error") != null ? "<p class='error'>Usuario o contraseña incorrectos</p>" : "";

            String html = """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                    <title>Login</title>
                    <link rel="stylesheet" href="/style.css">
                </head>
                <body>
                    <div class="login-container">
                        <h2>Iniciar Sesión</h2>
                        """ + errorMsg + """
                        <form method="post" action="/login">
                            <div class="form-group">
                                <label>Usuario:</label>
                                <input type="text" name="usuario" required>
                            </div>
                            <div class="form-group">
                                <label>Contraseña:</label>
                                <input type="password" name="password" required>
                            </div>
                            <button type="submit">Entrar</button>
                        </form>
                    </div>
                </body>
                </html>
                """;
            ctx.html(html);
        });

        // 2) Proceso POST para validar el formulario
        app.post("/login", ctx -> {
            String usuario = ctx.formParam("usuario");
            String password = ctx.formParam("password");

            // Validación de ejemplo (Credenciales: admin / 1234)
            if ("admin".equals(usuario) && "1234".equals(password)) {
                // Guardamos el usuario en la sesión y redirigimos a la raíz (/)
                ctx.sessionAttribute("usuario", usuario);
                ctx.redirect("/");
            } else {
                // Falla la autenticación, redirige al login con parámetro de error
                ctx.redirect("/login?error=true");
            }
        });

        // Ruta GET para la página de inicio (Sólo accesible si el filtro te deja pasar)
        app.get("/", ctx -> {
            String usuario = ctx.sessionAttribute("usuario");
            String html = """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                    <title>Inicio</title>
                    <link rel="stylesheet" href="/style.css">
                </head>
                <body>
                    <div class="home-container">
                        <h1>¡Bienvenido al sistema, """ + usuario + """
                        !</h1>
                        <p>Has pasado el filtro de autenticación exitosamente.</p>
                        <a href="/logout" class="btn-logout">Cerrar Sesión</a>
                    </div>
                </body>
                </html>
                """;
            ctx.html(html);
        });

        // Ruta auxiliar para cerrar sesión
        app.get("/logout", ctx -> {
            ctx.req().getSession().invalidate();
            ctx.redirect("/login");
        });
    }
}