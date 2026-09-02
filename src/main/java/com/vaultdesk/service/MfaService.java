package com.vaultdesk.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;

/**
 * Time-based One-Time Password (TOTP, RFC 6238) implementation, compatible
 * with standard authenticator apps (Google Authenticator, Authy, etc.) via
 * manual "enter setup key" entry - no QR code library dependency needed.
 *
 * Flow: generateSecret() -> show the Base32 string to the user -> they add
 * it as a "time-based" key in their authenticator app -> verifyCode()
 * checks what they type against the current 30-second window (with +-1
 * step of drift tolerance).
 */
public class MfaService {

    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private final SecureRandom secureRandom = new SecureRandom();

    /** Generates a new random 20-byte (160-bit) secret, Base32-encoded for display/entry. */
    public String generateSecret() {
        byte[] bytes = new byte[20];
        secureRandom.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /** Checks a 6-digit code against the current time step, allowing +-1 step of clock drift. */
    public boolean verifyCode(String base32Secret, String code) {
        if (code == null || !code.matches("\\d{6}")) return false;
        byte[] key = base32Decode(base32Secret);
        long currentStep = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;
        for (long delta = -1; delta <= 1; delta++) {
            String expected = generateCode(key, currentStep + delta);
            if (expected.equals(code)) return true;
        }
        return false;
    }

    private String generateCode(byte[] key, long timeStep) {
        try {
            byte[] data = ByteBuffer.allocate(8).putLong(timeStep).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);

            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%0" + CODE_DIGITS + "d", otp);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate TOTP code", e);
        }
    }

    private String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int bits = 0, value = 0;
        for (byte b : data) {
            value = (value << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                sb.append(BASE32_ALPHABET.charAt((value >>> (bits - 5)) & 0x1F));
                bits -= 5;
            }
        }
        if (bits > 0) {
            sb.append(BASE32_ALPHABET.charAt((value << (5 - bits)) & 0x1F));
        }
        return sb.toString();
    }

    private byte[] base32Decode(String base32) {
        String clean = base32.trim().toUpperCase().replace("=", "");
        int bits = 0, value = 0;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (char c : clean.toCharArray()) {
            int idx = BASE32_ALPHABET.indexOf(c);
            if (idx < 0) continue;
            value = (value << 5) | idx;
            bits += 5;
            if (bits >= 8) {
                out.write((value >>> (bits - 8)) & 0xFF);
                bits -= 8;
            }
        }
        return out.toByteArray();
    }
}
