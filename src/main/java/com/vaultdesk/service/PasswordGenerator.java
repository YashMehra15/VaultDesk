package com.vaultdesk.service;

import java.security.SecureRandom;

public class PasswordGenerator {

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{}?";

    private final SecureRandom random = new SecureRandom();

    public String generate(int length, boolean useUpper, boolean useLower,
                            boolean useDigits, boolean useSymbols) {
        StringBuilder pool = new StringBuilder();
        if (useUpper) pool.append(UPPER);
        if (useLower) pool.append(LOWER);
        if (useDigits) pool.append(DIGITS);
        if (useSymbols) pool.append(SYMBOLS);

        if (pool.length() == 0) {
            throw new IllegalArgumentException("At least one character set must be selected");
        }
        if (length < 4) {
            throw new IllegalArgumentException("Password length must be at least 4");
        }

        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            result.append(pool.charAt(random.nextInt(pool.length())));
        }
        return result.toString();
    }

    public int estimateStrength(String password) {
        int score = 0;
        if (password.length() >= 8) score += 20;
        if (password.length() >= 12) score += 20;
        if (password.matches(".*[A-Z].*")) score += 15;
        if (password.matches(".*[a-z].*")) score += 15;
        if (password.matches(".*[0-9].*")) score += 15;
        if (password.matches(".*[^A-Za-z0-9].*")) score += 15;
        return Math.min(score, 100);
    }

    /** Structured breakdown used by the per-entry strength display in the Passwords tab. */
    public static class Breakdown {
        public int score;
        public boolean hasLength8;
        public boolean hasLength12;
        public boolean hasUpper;
        public boolean hasLower;
        public boolean hasDigit;
        public boolean hasSymbol;
        public String label; // Weak / Fair / Strong
    }

    public Breakdown analyze(String password) {
        Breakdown b = new Breakdown();
        b.hasLength8 = password.length() >= 8;
        b.hasLength12 = password.length() >= 12;
        b.hasUpper = password.matches(".*[A-Z].*");
        b.hasLower = password.matches(".*[a-z].*");
        b.hasDigit = password.matches(".*[0-9].*");
        b.hasSymbol = password.matches(".*[^A-Za-z0-9].*");
        b.score = estimateStrength(password);
        b.label = b.score >= 75 ? "Strong" : (b.score >= 50 ? "Fair" : "Weak");
        return b;
    }
}
