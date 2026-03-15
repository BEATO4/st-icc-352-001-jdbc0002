package org.pucmm.eventos.utils;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilidad para hashear y verificar contraseñas con BCrypt.
 */
public class PasswordUtil {

    private static final int BCRYPT_ROUNDS = 12;

    private PasswordUtil() {}

    /** Genera el hash BCrypt de una contraseña en texto plano */
    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    /** Verifica si una contraseña en texto plano corresponde al hash almacenado */
    public static boolean verify(String plainPassword, String hashedPassword) {
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }
}