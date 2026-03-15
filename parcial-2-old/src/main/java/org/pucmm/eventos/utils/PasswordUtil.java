package org.pucmm.eventos.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {
    private PasswordUtil() {}

    public static String hash(String plainText) {
        return BCrypt.hashpw(plainText, BCrypt.gensalt(12));
    }

    public static boolean verify(String plainText, String hashed) {
        try {
            return BCrypt.checkpw(plainText, hashed);
        } catch (Exception e) {
            return false;
        }
    }
}