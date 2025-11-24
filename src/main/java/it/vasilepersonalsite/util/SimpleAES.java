package it.vasilepersonalsite.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SimpleAES {

    private static SecretKeySpec getKey(String myKey) {
        byte[] key = myKey.getBytes(StandardCharsets.UTF_8);

        byte[] finalKey = new byte[16];  // AES-128
        System.arraycopy(key, 0, finalKey, 0, Math.min(key.length, finalKey.length));

        return new SecretKeySpec(finalKey, "AES");
    }

    public static String cripta(String plainText, String key) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, getKey(key));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Errore durante la cifratura", e);
        }
    }

    public static String decripta(String encryptedText, String key) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, getKey(key));
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Errore durante la decifratura", e);
        }
    }
}

