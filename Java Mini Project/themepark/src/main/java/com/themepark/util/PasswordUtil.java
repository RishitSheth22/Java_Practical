package com.themepark.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/** Salted SHA-256 password hashing. Stored format:  salt$hexhash  */
public final class PasswordUtil {
    private static final SecureRandom RNG = new SecureRandom();

    private PasswordUtil() {}

    public static String hash(String password) {
        byte[] s = new byte[8];
        RNG.nextBytes(s);
        String salt = HexFormat.of().formatHex(s);
        return salt + "$" + sha256(salt + password);
    }

    public static boolean matches(String password, String stored) {
        if (stored == null) return false;
        int i = stored.indexOf('$');
        if (i < 0) return false;
        String salt = stored.substring(0, i);
        String expected = stored.substring(i + 1);
        return MessageDigest.isEqual(
                sha256(salt + password).getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
