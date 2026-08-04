package com.pvz2.service;

import com.pvz2.exception.AuthException;
import com.pvz2.exception.ValidationException;
import com.pvz2.model.User;
import com.pvz2.model.enums.Gender;
import com.pvz2.model.enums.SecurityQuestion;
import com.pvz2.repository.UserRepository;
import com.pvz2.util.HashUtil;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * سرویس احراز هویت: ثبت‌نام، ورود، بازیابی رمز.
 */
public class AuthService {

    private static final int MIN_NICKNAME_LEN = 3;
    private static final int MAX_NICKNAME_LEN = 30;
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9\\-]+$");
    private static final Pattern EMAIL_LOCAL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._\\-]+$");
    private static final Pattern EMAIL_DOMAIN_PATTERN =
            Pattern.compile("^[a-zA-Z0-9\\-]+(\\.[a-zA-Z0-9\\-]+)*$");
    private static final String SPECIAL_CHARS = "!#$%^&*()=+}{}[]|\\/;:'\".,><?";

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(String username, String password, String confirmPassword,
                         String nickname, String email, Gender gender) {
        validateUsername(username);
        validatePassword(password);
        if (!password.equals(confirmPassword)) {
            throw new ValidationException(
                "Password and confirmation do not match.");
        }
        validateNickname(nickname);
        validateEmail(email);
        if (userRepository.existsByUsername(username)) {
            throw new ValidationException(
                "Username '" + username + "' is already taken.");
        }
        String hash = HashUtil.sha256(password);
        User user = new User(username, hash, nickname, email, gender);
        user.setUnlockedPlants(new ArrayList<>());
        user.setSeenZombies(new ArrayList<>());
        user.setUnreadNews(new ArrayList<>());
        userRepository.save(user);
        return user;
    }

    private void validateUsername(String username) {
        if (username == null || username.isEmpty()) {
            throw new ValidationException("Username cannot be empty.");
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new ValidationException(
                "Username may only contain letters, digits, and hyphens.");
        }
    }

    private void validateNickname(String nickname) {
        if (nickname == null) {
            throw new ValidationException("Nickname cannot be null.");
        }
        if (nickname.length() < MIN_NICKNAME_LEN
                || nickname.length() > MAX_NICKNAME_LEN) {
            throw new ValidationException(
                "Nickname must be 3-30 characters long.");
        }
    }

    public void validateEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new ValidationException("Invalid email: must contain @.");
        }
        String[] parts = email.split("@", -1);
        if (parts.length != 2) {
            throw new ValidationException("Invalid email: must have exactly one @.");
        }
        validateEmailLocal(parts[0]);
        validateEmailDomain(parts[1]);
    }

    private void validateEmailLocal(String local) {
        if (local.isEmpty() || !local.matches("[a-zA-Z0-9].*[a-zA-Z0-9]")
                && local.length() > 1) {
            throw new ValidationException(
                "Email local part must start and end with letter/digit.");
        }
        if (local.contains("..")) {
            throw new ValidationException(
                "Email local part cannot have consecutive dots.");
        }
        if (!EMAIL_LOCAL_PATTERN.matcher(local).matches()) {
            throw new ValidationException(
                "Email local part contains invalid characters.");
        }
    }

    private void validateEmailDomain(String domain) {
        if (!domain.contains(".")) {
            throw new ValidationException(
                "Email domain must contain at least one dot.");
        }
        String[] domainParts = domain.split("\\.");
        String tld = domainParts[domainParts.length - 1];
        if (tld.length() < 2) {
            throw new ValidationException(
                "Email domain TLD must be at least 2 characters.");
        }
        if (domain.contains("..")) {
            throw new ValidationException(
                "Email domain cannot have consecutive dots.");
        }
        if (!EMAIL_DOMAIN_PATTERN.matcher(domain).matches()) {
            throw new ValidationException(
                "Email domain contains invalid characters.");
        }
    }

    public void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new ValidationException(
                "Password must be at least 8 characters long.");
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            }
            if (Character.isLowerCase(c)) {
                hasLower = true;
            }
            if (Character.isDigit(c)) {
                hasDigit = true;
            }
            if (SPECIAL_CHARS.indexOf(c) >= 0) {
                hasSpecial = true;
            }
        }
        if (!hasUpper) {
            throw new ValidationException(
                "Password must contain at least one uppercase letter.");
        }
        if (!hasLower) {
            throw new ValidationException(
                "Password must contain at least one lowercase letter.");
        }
        if (!hasDigit) {
            throw new ValidationException(
                "Password must contain at least one digit.");
        }
        if (!hasSpecial) {
            throw new ValidationException(
                "Password must contain at least one special character.");
        }
    }

    public void setSecurityQuestion(String username, SecurityQuestion question,
                                    String answer, String confirmAnswer) {
        if (!answer.equals(confirmAnswer)) {
            throw new ValidationException(
                "Answer and confirmation do not match.");
        }
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new AuthException("User not found.");
        }
        user.setSecurityQuestion(question);
        user.setSecurityAnswerHash(HashUtil.sha256(answer.toLowerCase()));
        userRepository.save(user);
    }

    public User login(String username, String password, boolean stayLoggedIn) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new AuthException("Username not found.");
        }
        if (!HashUtil.verify(password, user.getPasswordHash())) {
            throw new AuthException("Incorrect password.");
        }
        user.setStayLoggedIn(stayLoggedIn);
        userRepository.save(user);
        return user;
    }

    public SecurityQuestion initiatePasswordRecovery(String username, String email) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new AuthException("Username not found.");
        }
        if (!user.getEmail().equals(email)) {
            throw new AuthException("Email does not match account.");
        }
        return user.getSecurityQuestion();
    }

    public boolean verifySecurityAnswer(String username, String answer) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return false;
        }
        String hash = HashUtil.sha256(answer.toLowerCase());
        return hash.equals(user.getSecurityAnswerHash());
    }

    public void resetPassword(String username, String newPassword) {
        validatePassword(newPassword);
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new AuthException("User not found.");
        }
        user.setPasswordHash(HashUtil.sha256(newPassword));
        userRepository.save(user);
    }

    public boolean isValidUsername(String username) {
        return username != null
               && USERNAME_PATTERN.matcher(username).matches();
    }

    public boolean isValidEmail(String email) {
        try {
            validateEmail(email);
            return true;
        } catch (ValidationException e) {
            return false;
        }
    }

    public String hashPassword(String password) {
        return HashUtil.sha256(password);
    }
}
