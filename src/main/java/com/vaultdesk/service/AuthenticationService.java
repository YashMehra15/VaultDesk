package com.vaultdesk.service;

import com.vaultdesk.dao.SecurityLogDAO;
import com.vaultdesk.dao.UserDAO;
import com.vaultdesk.model.User;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

public class AuthenticationService {

    private static final AuthenticationService INSTANCE = new AuthenticationService();

    public static AuthenticationService getInstance() {
        return INSTANCE;
    }

    private static final int ITERATIONS = 100_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;

    /** After this many consecutive failed attempts, the account is temporarily locked. */
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 5;

    private final UserDAO userDAO = new UserDAO();
    private final SecurityLogDAO securityLogDAO = new SecurityLogDAO();
    private final SecureRandom secureRandom = new SecureRandom();

    /** Result of a login attempt - richer than a plain boolean so the UI can react correctly. */
    public static class LoginResult {
        public enum Status { SUCCESS, INVALID_CREDENTIALS, ACCOUNT_LOCKED, MFA_REQUIRED }
        public Status status;
        public User user;
        public LocalDateTime lockedUntil;

        static LoginResult of(Status status) {
            LoginResult r = new LoginResult();
            r.status = status;
            return r;
        }
    }

    // Holds state between "password verified, MFA required" and "MFA code confirmed" -
    // a real multi-user server would key this by session token; a single-user desktop
    // app only ever has one login in flight, so a simple instance field is sufficient.
    private User pendingMfaUser;
    private char[] pendingMfaPassword;

    public Optional<User> register(String username, char[] masterPassword) {
        if (userDAO.findByUsername(username).isPresent()) {
            return Optional.empty();
        }
        String hash = hashPassword(masterPassword);
        User user = userDAO.createUser(username, hash);
        return Optional.of(user);
    }

    public LoginResult login(String username, char[] masterPassword) {
        Optional<User> maybeUser = userDAO.findByUsername(username);
        if (maybeUser.isEmpty()) {
            return LoginResult.of(LoginResult.Status.INVALID_CREDENTIALS);
        }
        User user = maybeUser.get();

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            LoginResult r = LoginResult.of(LoginResult.Status.ACCOUNT_LOCKED);
            r.lockedUntil = user.getLockedUntil();
            return r;
        }

        if (!verifyPassword(masterPassword, user.getPasswordHash())) {
            userDAO.incrementFailedAttempts(user.getUserId());
            securityLogDAO.record(user.getUserId(), "Failed login attempt");

            int attemptsNow = user.getFailedAttempts() + 1;
            if (attemptsNow >= MAX_FAILED_ATTEMPTS) {
                LocalDateTime until = LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES);
                userDAO.lockUntil(user.getUserId(), until);
                securityLogDAO.record(user.getUserId(),
                        "Account locked for " + LOCKOUT_MINUTES + " minutes after " + attemptsNow + " failed attempts");
                LoginResult r = LoginResult.of(LoginResult.Status.ACCOUNT_LOCKED);
                r.lockedUntil = until;
                return r;
            }
            return LoginResult.of(LoginResult.Status.INVALID_CREDENTIALS);
        }

        // Correct password.
        userDAO.resetFailedAttempts(user.getUserId());

        if (user.isMfaEnabled()) {
            pendingMfaUser = user;
            pendingMfaPassword = masterPassword;
            LoginResult r = LoginResult.of(LoginResult.Status.MFA_REQUIRED);
            r.user = user;
            return r;
        }

        SessionManager.getInstance().startSession(user, new EncryptionService(masterPassword));
        securityLogDAO.record(user.getUserId(), "Successful login");
        LoginResult r = LoginResult.of(LoginResult.Status.SUCCESS);
        r.user = user;
        return r;
    }

    /** Second step of login when MFA is enabled - call after login() returned MFA_REQUIRED. */
    public boolean verifyMfaAndCompleteLogin(String code, MfaService mfaService) {
        if (pendingMfaUser == null || pendingMfaPassword == null) return false;
        boolean valid = mfaService.verifyCode(pendingMfaUser.getMfaSecret(), code);
        if (valid) {
            SessionManager.getInstance().startSession(pendingMfaUser, new EncryptionService(pendingMfaPassword));
            securityLogDAO.record(pendingMfaUser.getUserId(), "Successful login (MFA verified)");
        } else {
            securityLogDAO.record(pendingMfaUser.getUserId(), "Failed MFA code entry");
        }
        pendingMfaUser = null;
        pendingMfaPassword = null;
        return valid;
    }

    public void cancelPendingMfaLogin() {
        pendingMfaUser = null;
        pendingMfaPassword = null;
    }

    public String hashPassword(char[] password) {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        secureRandom.nextBytes(salt);
        byte[] hash = pbkdf2(password, salt);
        return ITERATIONS + ":" + b64(salt) + ":" + b64(hash);
    }

    public boolean verifyPassword(char[] password, String stored) {
        try {
            String[] parts = stored.split(":");
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[2]);
            byte[] actualHash = pbkdf2(password, salt, iterations);
            return Arrays.equals(expectedHash, actualHash);
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] pbkdf2(char[] password, byte[] salt) {
        return pbkdf2(password, salt, ITERATIONS);
    }

    private byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS);
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }

    private static String b64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }
}
