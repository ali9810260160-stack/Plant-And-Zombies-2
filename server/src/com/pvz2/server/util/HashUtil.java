package com.pvz2.server.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * SHA-256 password hashing — byte-for-byte identical to the client's
 * {@code com.pvz2.util.HashUtil} (lowercase hex of the UTF-8 digest) so the
 * server can validate accounts imported from the phase-1 {@code users.json}.
 */
public final class HashUtil {

    private HashUtil() { }

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public static boolean verify(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) return false;
        return sha256(rawPassword).equals(storedHash);
    }
}
