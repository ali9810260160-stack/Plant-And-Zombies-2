package model;

import model.enums.Gender;
import model.enums.SecurityQuestion;

import java.util.List;

/**
 * مدل کاربر بازی.
 * شامل اطلاعات احراز هویت، پروفایل و آمار بازی.
 */
public class User {

    /** شناسه یکتا کاربر */
    private String username;

    /** رمز عبور هش‌شده با SHA-256 */
    private String passwordHash;

    /** نام مستعار (نام نمایشی) */
    private String nickname;

    /** آدرس ایمیل */
    private String email;

    /** جنسیت */
    private Gender gender;

    /** سوال امنیتی انتخاب‌شده */
    private SecurityQuestion securityQuestion;

    /** پاسخ هش‌شده سوال امنیتی */
    private String securityAnswerHash;

    /** آیا کاربر stay-logged-in فعال کرده */
    private boolean stayLoggedIn;

    /** تعداد بازی‌های انجام‌شده */
    private int gamesPlayed;

    /** مجموع سکه‌های کسب‌شده */
    private long coins;

    /** مجموع الماس‌های کسب‌شده */
    private int gems;

    /** تعداد مراحل تکمیل‌شده */
    private int levelsCompleted;

    /** بیشترین میوپوینت در بازی امتیازی */
    private long highestMeoPoint;

    /** تعداد مینی‌گیم‌های موفق */
    private int minigamesCompleted;

    /** تعداد کوئست‌های روزانه تکمیل‌شده */
    private int dailyQuestsCompleted;

    /** تعداد کوئست‌های غیر روزانه تکمیل‌شده */
    private int regularQuestsCompleted;

    /** آخرین فصل و مرحله رسیده‌شده (برای لیدربورد) */
    private String lastReachedLevel;
    /** مراحل باز‌شده: فرمت "ANCIENT_EGYPT_1"، "FROSTBITE_CAVES_2" و ... */
    private java.util.List<String> unlockedLevels;

    /** میزان سختی انتخابی (1 تا 5، پیش‌فرض 3) */
    private int difficultyLevel;

    /** لیست گیاهان آنلاک‌شده */
    private List<String> unlockedPlants;

    /** لیست زامبی‌های مشاهده‌شده */
    private List<String> seenZombies;

    /** موجودی گلدان (برای گلخانه) */
    private int pots;

    /** تعداد غذای گیاه ذخیره‌شده (حداکثر 3) */
    private int plantFoodCount;

    /** لیست اخبار خوانده‌نشده */
    private List<NewsItem> unreadNews;

    /** تاریخ آخرین خرید پیشنهاد روزانه فروشگاه */
    private String lastDailyOfferDate;

    // ---- Constructor ----

    /** سازنده کاربر جدید با مقادیر پیش‌فرض */
    public User(String username, String passwordHash, String nickname,
                String email, Gender gender) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.email = email;
        this.gender = gender;
        this.difficultyLevel = 3;
        this.coins = 0;
        this.gems = 0;
        this.stayLoggedIn = false;
    }

    // ---- Getters & Setters ----

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public SecurityQuestion getSecurityQuestion() { return securityQuestion; }
    public void setSecurityQuestion(SecurityQuestion securityQuestion) {
        this.securityQuestion = securityQuestion;
    }

    public String getSecurityAnswerHash() { return securityAnswerHash; }
    public void setSecurityAnswerHash(String securityAnswerHash) {
        this.securityAnswerHash = securityAnswerHash;
    }

    public boolean isStayLoggedIn() { return stayLoggedIn; }
    public void setStayLoggedIn(boolean stayLoggedIn) { this.stayLoggedIn = stayLoggedIn; }

    public int getGamesPlayed() { return gamesPlayed; }
    public void setGamesPlayed(int gamesPlayed) { this.gamesPlayed = gamesPlayed; }

    public long getCoins() { return coins; }
    public void setCoins(long coins) { this.coins = coins; }

    public int getGems() { return gems; }
    public void setGems(int gems) { this.gems = gems; }

    public int getLevelsCompleted() { return levelsCompleted; }
    public void setLevelsCompleted(int levelsCompleted) {
        this.levelsCompleted = levelsCompleted;
    }

    public long getHighestMeoPoint() { return highestMeoPoint; }
    public void setHighestMeoPoint(long highestMeoPoint) {
        this.highestMeoPoint = highestMeoPoint;
    }

    public int getMinigamesCompleted() { return minigamesCompleted; }
    public void setMinigamesCompleted(int minigamesCompleted) {
        this.minigamesCompleted = minigamesCompleted;
    }

    public int getDailyQuestsCompleted() { return dailyQuestsCompleted; }
    public void setDailyQuestsCompleted(int dailyQuestsCompleted) {
        this.dailyQuestsCompleted = dailyQuestsCompleted;
    }

    public int getRegularQuestsCompleted() { return regularQuestsCompleted; }
    public void setRegularQuestsCompleted(int regularQuestsCompleted) {
        this.regularQuestsCompleted = regularQuestsCompleted;
    }

    public String getLastReachedLevel() { return lastReachedLevel; }
    public void setLastReachedLevel(String lastReachedLevel) {
        this.lastReachedLevel = lastReachedLevel;
    }

    public int getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(int difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public List<String> getUnlockedPlants() { return unlockedPlants; }
    public void setUnlockedPlants(List<String> unlockedPlants) {
        this.unlockedPlants = unlockedPlants;
    }

    public List<String> getSeenZombies() { return seenZombies; }
    public void setSeenZombies(List<String> seenZombies) {
        this.seenZombies = seenZombies;
    }

    public int getPots() { return pots; }
    public void setPots(int pots) { this.pots = pots; }

    public int getPlantFoodCount() { return plantFoodCount; }
    public void setPlantFoodCount(int plantFoodCount) {
        this.plantFoodCount = plantFoodCount;
    }

    public List<NewsItem> getUnreadNews() { return unreadNews; }
    public void setUnreadNews(List<NewsItem> unreadNews) {
        this.unreadNews = unreadNews;
    }

    public String getLastDailyOfferDate() { return lastDailyOfferDate; }
    public void setLastDailyOfferDate(String lastDailyOfferDate) {
        this.lastDailyOfferDate = lastDailyOfferDate;
    }


    private Greenhouse greenhouse;
    private java.util.List<NewsItem> allNews = new java.util.ArrayList<>();

    public int getNightOpsSun() { return 150; }
    public Greenhouse getGreenhouse() { return greenhouse; }
    public void setGreenhouse(Greenhouse g) { this.greenhouse = g; }
    public java.util.List<NewsItem> getAllNews() {
        if (allNews == null) allNews = new java.util.ArrayList<>();
        return allNews;
    }

    public java.util.List<String> getUnlockedLevels() {
        if (unlockedLevels == null) {
            unlockedLevels = new java.util.ArrayList<>();
            unlockedLevels.add("ANCIENT_EGYPT_1");
        }
        return unlockedLevels;
    }

    public void setUnlockedLevels(java.util.List<String> levels) {
        this.unlockedLevels = levels;
    }

    public boolean isLevelUnlocked(String chapterName, int levelNumber) {
        String key = chapterName.toUpperCase() + "_" + levelNumber;
        return getUnlockedLevels().contains(key);
    }

    public void unlockLevel(String chapterName, int levelNumber) {
        String key = chapterName.toUpperCase() + "_" + levelNumber;
        if (!getUnlockedLevels().contains(key)) {
            getUnlockedLevels().add(key);
        }
    }
}
