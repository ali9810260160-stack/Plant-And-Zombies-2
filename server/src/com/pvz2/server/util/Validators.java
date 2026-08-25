package com.pvz2.server.util;

import java.util.regex.Pattern;

/**
 * Server-side input validation mirroring the client's {@code AuthService} rules,
 * so the server independently enforces them (never trust the client).
 *
 * <p>Each check throws {@link ValidationError} with a stable code + message.
 */
public final class Validators {

    private Validators() { }

    /** Thrown when a field fails validation. */
    public static final class ValidationError extends RuntimeException {
        public final String code;
        public ValidationError(String code, String message) {
            super(message);
            this.code = code;
        }
    }

    private static final int MIN_NICKNAME_LEN = 3;
    private static final int MAX_NICKNAME_LEN = 30;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-]+$");
    private static final Pattern EMAIL_LOCAL_PATTERN = Pattern.compile("^[a-zA-Z0-9._\\-]+$");
    private static final Pattern EMAIL_DOMAIN_PATTERN =
            Pattern.compile("^[a-zA-Z0-9\\-]+(\\.[a-zA-Z0-9\\-]+)*$");
    private static final String SPECIAL_CHARS = "!#$%^&*()=+}{}[]|\\/;:'\".,><?";

    public static void username(String username) {
        if (username == null || username.isEmpty())
            throw new ValidationError("USERNAME_EMPTY", "Username cannot be empty.");
        if (!USERNAME_PATTERN.matcher(username).matches())
            throw new ValidationError("USERNAME_INVALID",
                    "Username may only contain letters, digits, and hyphens.");
    }

    public static void nickname(String nickname) {
        if (nickname == null)
            throw new ValidationError("NICKNAME_NULL", "Nickname cannot be null.");
        if (nickname.length() < MIN_NICKNAME_LEN || nickname.length() > MAX_NICKNAME_LEN)
            throw new ValidationError("NICKNAME_LEN", "Nickname must be 3-30 characters long.");
    }

    public static void password(String password) {
        if (password == null || password.length() < 8)
            throw new ValidationError("PASSWORD_WEAK",
                    "Password must be at least 8 characters long.");
        boolean up = false, low = false, dig = false, spec = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) up = true;
            else if (Character.isLowerCase(c)) low = true;
            else if (Character.isDigit(c)) dig = true;
            if (SPECIAL_CHARS.indexOf(c) >= 0) spec = true;
        }
        if (!up)   throw new ValidationError("PASSWORD_UPPER", "Password must contain an uppercase letter.");
        if (!low)  throw new ValidationError("PASSWORD_LOWER", "Password must contain a lowercase letter.");
        if (!dig)  throw new ValidationError("PASSWORD_DIGIT", "Password must contain a digit.");
        if (!spec) throw new ValidationError("PASSWORD_SPECIAL", "Password must contain a special character.");
    }

    public static void email(String email) {
        if (email == null || !email.contains("@"))
            throw new ValidationError("EMAIL_INVALID", "Invalid email: must contain @.");
        String[] parts = email.split("@", -1);
        if (parts.length != 2)
            throw new ValidationError("EMAIL_INVALID", "Invalid email: must have exactly one @.");
        String local = parts[0], domain = parts[1];
        if (local.isEmpty()
                || (!local.matches("[a-zA-Z0-9].*[a-zA-Z0-9]") && local.length() > 1))
            throw new ValidationError("EMAIL_LOCAL",
                    "Email local part must start and end with letter/digit.");
        if (local.contains(".."))
            throw new ValidationError("EMAIL_LOCAL", "Email local part cannot have consecutive dots.");
        if (!EMAIL_LOCAL_PATTERN.matcher(local).matches())
            throw new ValidationError("EMAIL_LOCAL", "Email local part contains invalid characters.");
        if (!domain.contains("."))
            throw new ValidationError("EMAIL_DOMAIN", "Email domain must contain at least one dot.");
        String[] dp = domain.split("\\.");
        if (dp[dp.length - 1].length() < 2)
            throw new ValidationError("EMAIL_DOMAIN", "Email domain TLD must be at least 2 characters.");
        if (domain.contains(".."))
            throw new ValidationError("EMAIL_DOMAIN", "Email domain cannot have consecutive dots.");
        if (!EMAIL_DOMAIN_PATTERN.matcher(domain).matches())
            throw new ValidationError("EMAIL_DOMAIN", "Email domain contains invalid characters.");
    }
}
