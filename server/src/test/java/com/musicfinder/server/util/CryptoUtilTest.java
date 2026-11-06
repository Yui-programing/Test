package com.musicfinder.server.util;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;

class CryptoUtilTest {

    @Test
    void testAesEncryptionDecryption() {
        SecretKey key = CryptoUtil.generateAesKey(256);
        String original = "{\"action\":\"SEARCH_SONG\",\"data\":{\"keyword\":\"Son Tung\"}}";

        String encrypted = CryptoUtil.encryptAes(original, key);
        assertNotNull(encrypted);
        assertNotEquals(original, encrypted);

        String decrypted = CryptoUtil.decryptAes(encrypted, key);
        assertEquals(original, decrypted);
    }

    @Test
    void testRsaEncryptionDecryption() {
        KeyPair keyPair = CryptoUtil.generateRsaKeyPair(2048);
        String secretMessage = "MySuperSecretAESKey123456789012";

        String encrypted = CryptoUtil.encryptRsa(secretMessage, keyPair.getPublic());
        assertNotNull(encrypted);
        assertNotEquals(secretMessage, encrypted);

        String decrypted = CryptoUtil.decryptRsa(encrypted, keyPair.getPrivate());
        assertEquals(secretMessage, decrypted);
    }

    @Test
    void testPasswordUtil() {
        String raw = "StrongPassword@123";
        String hashed = PasswordUtil.hashPassword(raw);
        assertNotNull(hashed);
        assertTrue(PasswordUtil.verifyPassword(raw, hashed));
        assertFalse(PasswordUtil.verifyPassword("WrongPassword", hashed));
    }
}
