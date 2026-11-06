package com.musicfinder.server.util;

public class PasswordUtil {
    public static String hashPassword(String password) {
        return PasswordHash.hashPassword(password);
    }

    public static boolean verifyPassword(String password, String hash) {
        return PasswordHash.verifyPassword(password, hash);
    }
}
