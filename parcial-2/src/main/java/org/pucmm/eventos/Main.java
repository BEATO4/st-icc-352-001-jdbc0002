package org.pucmm.eventos;

import io.javalin.Javalin;

public class Main {
    public static void main(String[] args) {
        // puerto del docker)
        Javalin app = Javalin.create(config -> {

            // ruta de prueba

            config.routes.get("/", ctx -> {
                ctx.result("Sistema de Gestion y Control de Eventos Academicos esta live");
            });

        }).start(7070);
    }
}


