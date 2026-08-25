package com.pvz2.service;

import com.pvz2.exception.ValidationException;
import com.pvz2.model.User;
import com.pvz2.model.enums.PlantType;
import com.pvz2.repository.UserRepository;
import com.pvz2.util.HashUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * سرویس مدیریت کاربر — پروفایل، سکه، الماس، کلکسیون.
 */
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void changeUsername(User user, String newUsername) {
        if (user.getUsername().equals(newUsername)) {
            throw new ValidationException(
                "New username is the same as the current one.");
        }
        if (userRepository.existsByUsername(newUsername)) {
            throw new ValidationException(
                "Username '" + newUsername + "' is already taken.");
        }
        user.setUsername(newUsername);
        userRepository.save(user);
    }

    public void changeNickname(User user, String newNickname) {
        if (user.getNickname().equals(newNickname)) {
            throw new ValidationException(
                "New nickname is the same as current one.");
        }
        if (newNickname.length() < 3 || newNickname.length() > 30) {
            throw new ValidationException(
                "Nickname must be 3-30 characters.");
        }
        user.setNickname(newNickname);
        userRepository.save(user);
    }

    public void changeEmail(User user, String newEmail) {
        if (user.getEmail().equals(newEmail)) {
            throw new ValidationException(
                "New email is the same as current one.");
        }
        new AuthService(userRepository).validateEmail(newEmail);
        user.setEmail(newEmail);
        userRepository.save(user);
    }

    public void changePassword(User user, String oldPassword, String newPassword) {
        if (!HashUtil.verify(oldPassword, user.getPasswordHash())) {
            throw new ValidationException("Old password is incorrect.");
        }
        if (HashUtil.verify(newPassword, user.getPasswordHash())) {
            throw new ValidationException(
                "New password must be different from old password.");
        }
        new AuthService(userRepository).validatePassword(newPassword);
        user.setPasswordHash(HashUtil.sha256(newPassword));
        userRepository.save(user);
    }

    public void changeDifficulty(User user, int level) {
        if (level < 1 || level > 5) {
            throw new ValidationException(
                "Difficulty must be between 1 and 5.");
        }
        user.setDifficultyLevel(level);
        userRepository.save(user);
    }

    public void addCoins(User user, long amount) {
        user.setCoins(user.getCoins() + amount);
        userRepository.save(user);
    }

    public void addGems(User user, int amount) {
        user.setGems(user.getGems() + amount);
        userRepository.save(user);
    }

    public boolean spendCoins(User user, long amount) {
        if (user.getCoins() < amount) {
            return false;
        }
        user.setCoins(user.getCoins() - amount);
        userRepository.save(user);
        return true;
    }

    public boolean spendGems(User user, int amount) {
        if (user.getGems() < amount) {
            return false;
        }
        user.setGems(user.getGems() - amount);
        userRepository.save(user);
        return true;
    }

    public void unlockPlant(User user, PlantType type) {
        if (user.getUnlockedPlants() == null) {
            user.setUnlockedPlants(new ArrayList<>());
        }
        String name = type.name();
        if (!user.getUnlockedPlants().contains(name)) {
            user.getUnlockedPlants().add(name);
            userRepository.save(user);
        }
    }

    public boolean hasPlant(User user, PlantType type) {
        if (user.getUnlockedPlants() == null) {
            return false;
        }
        return user.getUnlockedPlants().contains(type.name());
    }

    public void markZombieSeen(User user, String zombieType) {
        if (user.getSeenZombies() == null) {
            user.setSeenZombies(new ArrayList<>());
        }
        if (!user.getSeenZombies().contains(zombieType)) {
            user.getSeenZombies().add(zombieType);
            userRepository.save(user);
        }
    }

    public void updateHighScore(User user, long score) {
        if (score > user.getHighestMeoPoint()) {
            user.setHighestMeoPoint(score);
            userRepository.save(user);
        }
    }

    public void incrementGamesPlayed(User user) {
        user.setGamesPlayed(user.getGamesPlayed() + 1);
        userRepository.save(user);
    }

    public void incrementLevelsCompleted(User user) {
        user.setLevelsCompleted(user.getLevelsCompleted() + 1);
        userRepository.save(user);
    }

    public void save(User user) {
        userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.getAllUsers();
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public UserRepository getUserRepository() { return userRepository; }

}
