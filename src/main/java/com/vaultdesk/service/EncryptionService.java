package com.vaultdesk.service;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Field-level AES-256-GCM encryption for vault items, keyed off the user's
 * master password via PBKDF2WithHmacSHA256 with a fresh random salt per
 * value. Stored format: "saltB64:ivB64:ciphertextB64". The master password
 * itself is never stored - only its PBKDF2 hash (see AuthenticationService).
 */
public class EncryptionService {

    private static final int PBKDF2_ITERATIONS = 65536;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final char[] masterPassword;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptionService(char[] masterPassword) {
        this.masterPassword = masterPassword;
    }

    public String encrypt(String plainText) {
        try {
            byte[] salt = new byte[SALT_LENGTH_BYTES];
            secureRandom.nextBytes(salt);
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            SecretKeySpec key = deriveKey(salt);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherBytes = cipher.doFinal(plainText.getBytes("UTF-8"));

            return b64(salt) + ":" + b64(iv) + ":" + b64(cipherBytes);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String stored) {
        try {
            String[] parts = stored.split(":");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Malformed encrypted payload");
            }
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] cipherBytes = Base64.getDecoder().decode(parts[2]);

            SecretKeySpec key = deriveKey(salt);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plainBytes = cipher.doFinal(cipherBytes);

            return new String(plainBytes, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed - wrong master password or corrupted data", e);
        }
    }

    private SecretKeySpec deriveKey(byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        PBEKeySpec spec = new PBEKeySpec(masterPassword, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    private static String b64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }
}
