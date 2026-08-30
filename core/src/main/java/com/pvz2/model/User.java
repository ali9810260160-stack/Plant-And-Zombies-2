package com.pvz2.model;

import com.pvz2.model.enums.Gender;
import com.pvz2.model.enums.PlantType;
import com.pvz2.model.enums.SecurityQuestion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * مدل کاربر بازی.
 * شامل اطلاعات احراز هویت، پروفایل، آمار بازی، سطح ارتقای گیاهان و موجودی بسته‌بذر.
 */
public class User {

    private String username;
    private String passwordHash;
    private String nickname;
    private String email;
    private Gender gender;
    private SecurityQuestion securityQuestion;
    private String securityAnswerHash;
    private boolean stayLoggedIn;
    private int gamesPlayed;
    private long coins;
    private int gems;
    private int levelsCompleted;
    private long highestMeoPoint;
    private int minigamesCompleted;
    private int dailyQuestsCompleted;
    private int regularQuestsCompleted;
    private String lastReachedLevel;
    private java.util.List<String> unlockedLevels;
    private int difficultyLevel;
    private List<String> unlockedPlants;
    private List<String> seenZombies;
    private int pots;
    private int plantFoodCount;
    private List<NewsItem> unreadNews;
    private String lastDailyOfferDate;
    private Greenhouse greenhouse;
    private java.util.List<NewsItem> allNews = new java.util.ArrayList<>();

    /**
     * تنظیماتِ صوت/گرافیکِ ذخیره‌شونده به‌ازای هر کاربر (فاز ۲).
     * این‌ها جدا برای هر user در users.json سیو می‌شوند و هنگام لاگین/auto-login
     * روی {@link GameSettings} اعمال می‌شوند.
     */
    private float masterVolume = 0.8f;
    private boolean musicEnabled = true;
    private boolean sfxEnabled = true;
    private boolean showGrid = false;
    private float gameSpeed = 1.0f;

    /**
     * سطح ارتقای هر گیاه: key = PlantType.name()، value = 0..3
     * 0 = بدون ارتقا، 1 = ارتقای اول، 2 = دوم، 3 = سوم (حداکثر)
     */
    private Map<String, Integer> plantUpgradeLevels = new HashMap<>();

    /**
     * موجودی بسته‌بذر هر گیاه: key = PlantType.name()، value = تعداد بسته
     */
    private Map<String, Integer> plantSeedPackets = new HashMap<>();

    /**
     * مجموعه‌ی گیاهانی که با الماس boost شده‌اند (دائمی): key = PlantType.name()
     * افزوده‌شده در فاز ۲ برای صفحه‌ی انتخاب گیاه — پرچمی جدا از سطح ارتقا.
     */
    private List<String> boostedPlants = new java.util.ArrayList<>();

    /** پیشنهاد روزانه فروشگاه: نام گیاه پیشنهادی امروز و تاریخی که تولید شده */
    private String dailyOfferPlant;
    private String dailyOfferGeneratedDate;

    /**
     * شناسه‌ی کوئست‌هایی که این کاربر تکمیل کرده (persist می‌شود تا پرچمِ
     * «Achieved» بین session‌ها بماند). کوئست‌های روزانه در ریستِ روزانه از این
     * مجموعه حذف می‌شوند تا دوباره قابلِ انجام باشند (منطق در QuestService).
     */
    private List<String> completedQuests = new java.util.ArrayList<>();

    // ---- Constructor ----

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
        this.plantUpgradeLevels = new HashMap<>();
        this.plantSeedPackets = new HashMap<>();
    }

    // ---- Plant Upgrade Level ----

    /** سطح ارتقای فعلی یک گیاه (0 = بدون ارتقا) */
    public int getPlantUpgradeLevel(String plantTypeName) {
        return plantUpgradeLevels.getOrDefault(plantTypeName.toUpperCase(), 0);
    }

    public int getPlantUpgradeLevel(PlantType type) {
        return getPlantUpgradeLevel(type.name());
    }

    /** افزایش سطح ارتقا (حداکثر ۳) — برمی‌گرداند سطح جدید */
    public int incrementPlantUpgradeLevel(String plantTypeName) {
        String key = plantTypeName.toUpperCase();
        int current = plantUpgradeLevels.getOrDefault(key, 0);
        int next = Math.min(3, current + 1);
        plantUpgradeLevels.put(key, next);
        return next;
    }

    public Map<String, Integer> getPlantUpgradeLevels() {
        if (plantUpgradeLevels == null) plantUpgradeLevels = new HashMap<>();
        return plantUpgradeLevels;
    }

    public void setPlantUpgradeLevels(Map<String, Integer> map) {
        this.plantUpgradeLevels = map != null ? map : new HashMap<>();
    }

    // ---- Seed Packets ----

    /** موجودی بسته‌بذر یک گیاه */
    public int getSeedPackets(String plantTypeName) {
        return plantSeedPackets.getOrDefault(plantTypeName.toUpperCase(), 0);
    }

    public int getSeedPackets(PlantType type) {
        return getSeedPackets(type.name());
    }

    /** اضافه کردن بسته‌بذر */
    public void addSeedPackets(String plantTypeName, int count) {
        String key = plantTypeName.toUpperCase();
        plantSeedPackets.merge(key, count, Integer::sum);
    }

    public void addSeedPackets(PlantType type, int count) {
        addSeedPackets(type.name(), count);
    }

    /** خرج کردن بسته‌بذر — false اگر کافی نباشد */
    public boolean spendSeedPackets(String plantTypeName, int count) {
        String key = plantTypeName.toUpperCase();
        int current = plantSeedPackets.getOrDefault(key, 0);
        if (current < count) return false;
        plantSeedPackets.put(key, current - count);
        return true;
    }

    public Map<String, Integer> getPlantSeedPackets() {
        if (plantSeedPackets == null) plantSeedPackets = new HashMap<>();
        return plantSeedPackets;
    }

    public void setPlantSeedPackets(Map<String, Integer> map) {
        this.plantSeedPackets = map != null ? map : new HashMap<>();
    }

    // ---- Boosted Plants (فاز ۲) ----

    public boolean isPlantBoosted(String plantTypeName) {
        return boostedPlants != null && boostedPlants.contains(plantTypeName.toUpperCase());
    }

    public boolean isPlantBoosted(PlantType type) { return isPlantBoosted(type.name()); }

    public void setPlantBoosted(String plantTypeName, boolean boosted) {
        if (boostedPlants == null) boostedPlants = new java.util.ArrayList<>();
        if (boosted) boostedPlants.add(plantTypeName.toUpperCase());
        else boostedPlants.remove(plantTypeName.toUpperCase());
    }

    public void setPlantBoosted(PlantType type, boolean boosted) { setPlantBoosted(type.name(), boosted); }

    public void consumeStoredBoost(String plantTypeName) {
        if (boostedPlants != null) boostedPlants.remove(plantTypeName.toUpperCase());
    }

    public List<String> getBoostedPlants() {
        if (boostedPlants == null) boostedPlants = new java.util.ArrayList<>();
        return boostedPlants;
    }

    public void setBoostedPlants(List<String> v) {
        this.boostedPlants = v != null ? v : new java.util.ArrayList<>();
    }

    // ---- Daily Shop Offer ----

    public String getDailyOfferPlant() { return dailyOfferPlant; }
    public void setDailyOfferPlant(String p) { this.dailyOfferPlant = p; }
    public String getDailyOfferGeneratedDate() { return dailyOfferGeneratedDate; }
    public void setDailyOfferGeneratedDate(String d) { this.dailyOfferGeneratedDate = d; }


    // ---- Level utilities ----

    public int getNightOpsSun() { return 150; }

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

    // ---- Standard Getters & Setters ----

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
    public void setSecurityQuestion(SecurityQuestion q) { this.securityQuestion = q; }
    public String getSecurityAnswerHash() { return securityAnswerHash; }
    public void setSecurityAnswerHash(String h) { this.securityAnswerHash = h; }
    public boolean isStayLoggedIn() { return stayLoggedIn; }
    public void setStayLoggedIn(boolean v) { this.stayLoggedIn = v; }
    public int getGamesPlayed() { return gamesPlayed; }
    public void setGamesPlayed(int v) { this.gamesPlayed = v; }
    public long getCoins() { return coins; }
    public void setCoins(long coins) { this.coins = coins; }
    public int getGems() { return gems; }
    public void setGems(int gems) { this.gems = gems; }
    public int getLevelsCompleted() { return levelsCompleted; }
    public void setLevelsCompleted(int v) { this.levelsCompleted = v; }
    public long getHighestMeoPoint() { return highestMeoPoint; }
    public void setHighestMeoPoint(long v) { this.highestMeoPoint = v; }
    public int getMinigamesCompleted() { return minigamesCompleted; }
    public void setMinigamesCompleted(int v) { this.minigamesCompleted = v; }
    public int getDailyQuestsCompleted() { return dailyQuestsCompleted; }
    public void setDailyQuestsCompleted(int v) { this.dailyQuestsCompleted = v; }
    public int getRegularQuestsCompleted() { return regularQuestsCompleted; }
    public void setRegularQuestsCompleted(int v) { this.regularQuestsCompleted = v; }

    public List<String> getCompletedQuests() {
        if (completedQuests == null) completedQuests = new java.util.ArrayList<>();
        return completedQuests;
    }
    public void setCompletedQuests(List<String> v) {
        this.completedQuests = v != null ? v : new java.util.ArrayList<>();
    }
    public String getLastReachedLevel() { return lastReachedLevel; }
    public void setLastReachedLevel(String v) { this.lastReachedLevel = v; }
    public int getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(int v) { this.difficultyLevel = v; }
    public List<String> getUnlockedPlants() { return unlockedPlants; }
    public void setUnlockedPlants(List<String> v) { this.unlockedPlants = v; }
    public List<String> getSeenZombies() { return seenZombies; }
    public void setSeenZombies(List<String> v) { this.seenZombies = v; }
    public int getPots() { return pots; }
    public void setPots(int pots) { this.pots = pots; }
    public int getPlantFoodCount() { return plantFoodCount; }
    public void setPlantFoodCount(int v) { this.plantFoodCount = v; }
    public List<NewsItem> getUnreadNews() { return unreadNews; }
    public void setUnreadNews(List<NewsItem> v) { this.unreadNews = v; }
    public String getLastDailyOfferDate() { return lastDailyOfferDate; }
    public void setLastDailyOfferDate(String v) { this.lastDailyOfferDate = v; }
    public Greenhouse getGreenhouse() { return greenhouse; }
    public void setGreenhouse(Greenhouse g) { this.greenhouse = g; }

    // ---- Per-user settings (فاز ۲) ----
    public float getMasterVolume() { return masterVolume; }
    public void setMasterVolume(float v) { this.masterVolume = Math.max(0f, Math.min(1f, v)); }
    public boolean isMusicEnabled() { return musicEnabled; }
    public void setMusicEnabled(boolean v) { this.musicEnabled = v; }
    public boolean isSfxEnabled() { return sfxEnabled; }
    public void setSfxEnabled(boolean v) { this.sfxEnabled = v; }
    public boolean isShowGrid() { return showGrid; }
    public void setShowGrid(boolean v) { this.showGrid = v; }
    public float getGameSpeed() { return gameSpeed; }
    public void setGameSpeed(float v) { this.gameSpeed = v; }
    public java.util.List<NewsItem> getAllNews() {
        if (allNews == null) allNews = new java.util.ArrayList<>();
        return allNews;
    }
}
