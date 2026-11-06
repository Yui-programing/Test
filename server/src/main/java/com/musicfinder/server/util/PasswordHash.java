package com.musicfinder.server.util;
import org.mindrot.jbcrypt.BCrypt;
public class PasswordHash {
    public static String hashPassword(String password){
        if(password == null || password.isBlank()){
            throw new IllegalArgumentException("Mật khẩu không được để trống");
        }
        return BCrypt.hashpw(password, BCrypt.gensalt(10));
    }

    //so sanh
    public static boolean verifyPassword(String password, String passwordHash){
        if(password == null || passwordHash == null || !passwordHash.startsWith("$2")){
            return false;
        }
        return BCrypt.checkpw(password, passwordHash);// tra ve false neu kh giong nhau
    }
}
