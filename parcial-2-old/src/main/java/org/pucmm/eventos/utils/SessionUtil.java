package org.pucmm.eventos.util;

import io.javalin.http.Context;
import org.pucmm.eventos.model.Role;
import org.pucmm.eventos.model.User;

public class SessionUtil {
    private SessionUtil() {}

    public static void setUser(Context ctx, User user) {
        ctx.sessionAttribute("userId",   user.getId());
        ctx.sessionAttribute("username", user.getUsername());
        ctx.sessionAttribute("userRole", user.getRole().name());
    }

    public static Long getUserId(Context ctx) {
        return ctx.sessionAttribute("userId");
    }

    public static String getUserRole(Context ctx) {
        return ctx.sessionAttribute("userRole");
    }

    public static boolean isLoggedIn(Context ctx) {
        return getUserId(ctx) != null;
    }

    public static boolean hasRole(Context ctx, Role role) {
        String r = getUserRole(ctx);
        return r != null && r.equals(role.name());
    }

    public static void clear(Context ctx) {
        ctx.req().getSession().invalidate();
    }
}