package com.pvz2.model;

import com.pvz2.model.enums.GameResult;
import com.pvz2.model.enums.PlantType;
import com.pvz2.model.zombies.Zombie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * وضعیت جاری یک مرحله در حال اجرا.
 * شامل state کامل هر 8 نوع مرحله ویژه.
 */
public class GameSession {

    private Level level;
    private GameMap gameMap;
    private int currentTick;
    private int sunAmount;
    private int plantFoodCount;
    private int currentWaveIndex;
    private List<Wave> waves;
    private List<Zombie> activeZombies;
    private List<Projectile> activeProjectiles;
    private List<Sun> activeSuns;
    private List<PlantType> selectedPlants;
    private List<PlantType> boostedPlants;
    private GameResult result;
    private boolean cooldownCheated;

    /** cooldownِ بسته‌ی بذرِ هر نوع گیاه پس از کاشت (بر حسبِ تیک). */
    private final java.util.Map<com.pvz2.model.enums.PlantType, Integer> seedCdRemaining =
            new java.util.EnumMap<>(com.pvz2.model.enums.PlantType.class);
    private final java.util.Map<com.pvz2.model.enums.PlantType, Integer> seedCdTotal =
            new java.util.EnumMap<>(com.pvz2.model.enums.PlantType.class);
    private int plantsLost;
    private int zombiesKilled;
    private long meoPoints;
    private boolean waveStarted;
    private int lastSkyDropTick;
    private boolean betweenWaves;
    private int levelNumber;
    private boolean egyptTornadoActive;
    private List<Integer> frostbiteWindAffectedRows;
    private Map<String, com.pvz2.model.plants.Plant> catPlants;
    private Object minigameState;
    /** رئیس (Zomboss) — فقط در مراحلِ BOSS غیر-null. نگاه کنید {@link Boss}. */
    private Boss boss;
    private int consecutiveKills;
    private long lastKillTick;

    // ─── الگوهای امتیازی (بازی امتیازی — Scored) ───────────────────────────────
    /** اعلان‌های الگوی میوپوینت که هنوز به لایه‌ی گرافیک تحویل داده نشده‌اند. */
    private final List<String> meoEvents = new ArrayList<>();
    /** پنجره‌ی جمع‌آوریِ آیتم (خورشید) — برای الگوی Item Collector. */
    private int itemWindowStartTick = -1;
    private int itemsInWindow;

    // ════════════════════════════════════════════════════════
    //  CONVEYOR_BELT state
    // ════════════════════════════════════════════════════════
    /** گیاهانی که روی نوار کناری آماده کاشت هستند */
    private List<PlantType> conveyorQueue;
    /** تیک آخرین گیاه نوار */
    private int lastConveyorTick;
    /** فاصله تیک بین گیاهان نوار (120 = 12 ثانیه) */
    private static final int CONVEYOR_INTERVAL_TICKS = 120;

    // ════════════════════════════════════════════════════════
    //  SAVE_OUR_SEEDS state
    // ════════════════════════════════════════════════════════
    /**
     * موقعیت‌های گیاهان محافظت‌شده: int[]{x, y}
     * اگر هر کدام خورده شوند → باخت فوری
     */
    private List<int[]> protectedPlantPositions;

    // ════════════════════════════════════════════════════════
    //  TIMED_WAR state
    // ════════════════════════════════════════════════════════
    /** تعداد زامبی‌های کشته‌شده برای هدف Timed War */
    private int timedWarKillsAchieved;
    /** مقدار خورشید تولیدشده برای هدف Timed War (حالت خورشید) */
    private int timedWarSunAchieved;
    /** آیا هدف Timed War محقق شده (WIN) */
    private boolean timedWarGoalReached;

    // ════════════════════════════════════════════════════════
    //  LOVE_YOUR_PLANTS state
    // ════════════════════════════════════════════════════════
    /** گیاهان اولیه‌ای که باید محافظت شوند (فقط برای نمایش) */
    private List<int[]> protectedLovePlantPositions;

    // ════════════════════════════════════════════════════════
    //  Constructor
    // ════════════════════════════════════════════════════════

    public GameSession(Level level, GameMap gameMap, List<PlantType> selectedPlants) {
        this.level = level;
        this.gameMap = gameMap;
        this.selectedPlants = new ArrayList<>(selectedPlants);
        this.currentTick = 0;
        this.result = GameResult.IN_PROGRESS;
        this.cooldownCheated = false;
        this.sunAmount = level.getInitialSunAmount() > 0
                ? level.getInitialSunAmount() : 50;
        this.plantFoodCount = 0;
        this.currentWaveIndex = 0;
        this.activeZombies = new ArrayList<>();
        this.activeProjectiles = new ArrayList<>();
        this.activeSuns = new ArrayList<>();
        this.waves = new ArrayList<>();
        this.boostedPlants = new ArrayList<>();
        this.plantsLost = 0;
        this.zombiesKilled = 0;
        this.meoPoints = 0;
        this.waveStarted = false;
        this.lastSkyDropTick = 0;
        this.betweenWaves = false;
        this.frostbiteWindAffectedRows = new ArrayList<>();
        this.catPlants = new HashMap<>();
        this.consecutiveKills = 0;
        this.lastKillTick = 0;

        // special level state init
        this.conveyorQueue = new ArrayList<>();
        this.lastConveyorTick = 0;
        this.protectedPlantPositions = new ArrayList<>();
        this.protectedLovePlantPositions = new ArrayList<>();
        this.timedWarKillsAchieved = 0;
        this.timedWarSunAchieved = 0;
        this.timedWarGoalReached = false;
    }

    // ════════════════════════════════════════════════════════
    //  General game methods
    // ════════════════════════════════════════════════════════

    public void advanceTick() { currentTick++; }

    public double getElapsedSeconds() { return currentTick / 10.0; }

    public boolean isInProgress() { return result == GameResult.IN_PROGRESS; }

    public void addSun(int amount) { sunAmount += amount; }

    // ─── VERSUS (فاز ۳): دو اقتصادِ مجزا + تایمر + برنده ───────────────────────
    /** خورشیدِ بازیکنِ زامبی (جدا از خورشیدِ گیاه‌کار = sunAmount). */
    private int zombieSun;
    /** تیک‌های باقی‌مانده تا پایانِ مسابقه (بردِ گیاه با اتمامِ زمان). */
    private int versusTicksLeft;
    /** نقشِ برنده وقتی مسابقه تمام شد: "PLANT" یا "ZOMBIE"؛ null یعنی هنوز ادامه دارد. */
    private String versusWinnerRole;

    public int getZombieSun() { return zombieSun; }
    public void setZombieSun(int v) { this.zombieSun = v; }
    public void addZombieSun(int v) { this.zombieSun += v; }
    public boolean spendZombieSun(int amount) {
        if (zombieSun < amount) return false;
        zombieSun -= amount;
        return true;
    }
    public int getVersusTicksLeft() { return versusTicksLeft; }
    public void setVersusTicksLeft(int v) { this.versusTicksLeft = v; }
    public String getVersusWinnerRole() { return versusWinnerRole; }
    public void setVersusWinnerRole(String r) { this.versusWinnerRole = r; }

    public boolean spendSun(int amount) {
        if (sunAmount < amount) return false;
        sunAmount -= amount;
        return true;
    }

    public boolean addPlantFood() {
        if (plantFoodCount >= 3) return false;
        plantFoodCount++;
        return true;
    }

    public boolean usePlantFood() {
        if (plantFoodCount <= 0) return false;
        plantFoodCount--;
        return true;
    }

    public Wave getCurrentWave() {
        if (waves == null || currentWaveIndex >= waves.size()) return null;
        return waves.get(currentWaveIndex);
    }

    public boolean hasMoreWaves() {
        return currentWaveIndex < waves.size() - 1;
    }

    public boolean allWavesFinished() {
        if (waves == null || waves.isEmpty()) return false;
        if (currentWaveIndex < waves.size() - 1) return false;
        return activeZombies.isEmpty();
    }

    // ════════════════════════════════════════════════════════
    //  CONVEYOR_BELT methods
    // ════════════════════════════════════════════════════════

    /** آیا زمان اضافه کردن گیاه جدید به نوار رسیده */
    public boolean isConveyorReady(int currentTick) {
        return currentTick == 0
                || (currentTick - lastConveyorTick) >= CONVEYOR_INTERVAL_TICKS;
    }

    /** اضافه کردن گیاه به نوار کناری */
    public void addToConveyor(PlantType type, int currentTick) {
        conveyorQueue.add(type);
        lastConveyorTick = currentTick;
    }

    /** برداشتن گیاه از نوار کناری برای کاشت */
    public boolean useConveyorPlant(PlantType type) {
        return conveyorQueue.remove(type);
    }

    /** آیا این نوع گیاه در نوار کناری موجود است */
    public boolean hasConveyorPlant(PlantType type) {
        return conveyorQueue.contains(type);
    }

    // ════════════════════════════════════════════════════════
    //  SAVE_OUR_SEEDS methods
    // ════════════════════════════════════════════════════════

    /** ثبت یک موقعیت به عنوان موقعیت گیاه محافظت‌شده */
    public void addProtectedPosition(int x, int y) {
        protectedPlantPositions.add(new int[]{x, y});
    }

    /** آیا این موقعیت یک گیاه محافظت‌شده دارد */
    public boolean isProtectedPosition(int x, int y) {
        for (int[] pos : protectedPlantPositions) {
            if (pos[0] == x && pos[1] == y) return true;
        }
        return false;
    }

    /** حذف موقعیت از لیست محافظت (وقتی گیاه خورده می‌شود) */
    public void removeProtectedPosition(int x, int y) {
        protectedPlantPositions.removeIf(p -> p[0] == x && p[1] == y);
    }

    // ════════════════════════════════════════════════════════
    //  TIMED_WAR methods
    // ════════════════════════════════════════════════════════

    /** ثبت کشتن زامبی برای Timed War */
    public void registerTimedWarKill() {
        timedWarKillsAchieved++;
    }

    /** ثبت خورشید تولیدشده برای Timed War (حالت خورشید) */
    public void registerTimedWarSun(int amount) {
        timedWarSunAchieved += amount;
    }

    /** ثانیه‌های باقیمانده Timed War */
    public int getTimedWarRemainingSeconds() {
        if (level == null) return 0;
        int totalTicks = level.getTimedWarSeconds() * 10;
        int elapsed = currentTick;
        int remaining = totalTicks - elapsed;
        return Math.max(0, remaining / 10);
    }

    /** بررسی اتمام زمان Timed War */
    public boolean isTimedWarExpired() {
        if (level == null) return false;
        return currentTick >= level.getTimedWarSeconds() * 10;
    }

    // ════════════════════════════════════════════════════════
    //  Standard getters/setters
    // ════════════════════════════════════════════════════════

    public Level getLevel() { return level; }
    public GameMap getGameMap() { return gameMap; }
    public int getCurrentTick() { return currentTick; }
    public void setCurrentTick(int tick) { this.currentTick = tick; }
    public int getSunAmount() { return sunAmount; }
    public void setSunAmount(int sunAmount) { this.sunAmount = sunAmount; }
    public int getPlantFoodCount() { return plantFoodCount; }
    public void setPlantFoodCount(int count) { this.plantFoodCount = count; }
    public int getCurrentWaveIndex() { return currentWaveIndex; }
    public void setCurrentWaveIndex(int idx) { this.currentWaveIndex = idx; }
    public List<Wave> getWaves() { return waves; }
    public void setWaves(List<Wave> waves) { this.waves = waves; }
    public List<Zombie> getActiveZombies() { return activeZombies; }
    public List<Projectile> getActiveProjectiles() { return activeProjectiles; }
    public List<Sun> getActiveSuns() { return activeSuns; }
    public List<PlantType> getSelectedPlants() { return selectedPlants; }
    public List<PlantType> getBoostedPlants() { return boostedPlants; }
    public void setBoostedPlants(List<PlantType> b) { this.boostedPlants = b; }
    public GameResult getResult() { return result; }
    public void setResult(GameResult result) { this.result = result; }
    // ─── cooldownِ بسته‌ی بذر (seed packet recharge) ─────────────────────────
    /** شروعِ cooldownِ بسته‌ی بذر پس از کاشتِ موفقِ این نوع گیاه. */
    public void startSeedCooldown(com.pvz2.model.enums.PlantType type, int ticks) {
        if (type == null || ticks <= 0) return;
        seedCdRemaining.put(type, ticks);
        seedCdTotal.put(type, ticks);
    }
    /** هر تیک: کاهشِ همه‌ی cooldownهای بسته‌ی بذر. */
    public void tickSeedCooldowns() {
        for (java.util.Map.Entry<com.pvz2.model.enums.PlantType, Integer> e
                : seedCdRemaining.entrySet()) {
            if (e.getValue() > 0) e.setValue(e.getValue() - 1);
        }
    }
    /** آیا بسته‌ی این گیاه آماده است (cooldown تمام شده)؟ */
    public boolean isSeedReady(com.pvz2.model.enums.PlantType type) {
        Integer r = seedCdRemaining.get(type);
        return r == null || r <= 0;
    }
    /** کسرِ باقی‌مانده‌ی cooldown (۰=آماده … ۱=تازه کاشته). */
    public float seedCooldownFraction(com.pvz2.model.enums.PlantType type) {
        Integer r = seedCdRemaining.get(type);
        Integer t = seedCdTotal.get(type);
        if (r == null || t == null || t <= 0 || r <= 0) return 0f;
        return Math.min(1f, (float) r / t);
    }
    public java.util.Map<com.pvz2.model.enums.PlantType, Integer> getSeedCdRemaining() {
        return seedCdRemaining;
    }

    public boolean isCooldownCheated() { return cooldownCheated; }
    public void setCooldownCheated(boolean b) { this.cooldownCheated = b; }
    public int getPlantsLost() { return plantsLost; }
    public void setPlantsLost(int plantsLost) { this.plantsLost = plantsLost; }
    public int getZombiesKilled() { return zombiesKilled; }
    public void setZombiesKilled(int zombiesKilled) { this.zombiesKilled = zombiesKilled; }
    public long getMeoPoints() { return meoPoints; }
    public void setMeoPoints(long meoPoints) { this.meoPoints = meoPoints; }
    public boolean isWaveStarted() { return waveStarted; }
    public void setWaveStarted(boolean waveStarted) { this.waveStarted = waveStarted; }
    public int getLastSkyDropTick() { return lastSkyDropTick; }
    public void setLastSkyDropTick(int t) { this.lastSkyDropTick = t; }
    public boolean isBetweenWaves() { return betweenWaves; }
    public void setBetweenWaves(boolean b) { this.betweenWaves = b; }
    public boolean isEgyptTornadoActive() { return egyptTornadoActive; }
    public void setEgyptTornadoActive(boolean b) { this.egyptTornadoActive = b; }
    public List<Integer> getFrostbiteWindAffectedRows() { return frostbiteWindAffectedRows; }
    public Map<String, com.pvz2.model.plants.Plant> getCatPlants() { return catPlants; }
    public Object getMinigameState() { return minigameState; }
    public void setMinigameState(Object s) { this.minigameState = s; }
    public Boss getBoss() { return boss; }
    public void setBoss(Boss boss) { this.boss = boss; }
    public int getConsecutiveKills() { return consecutiveKills; }
    public void setConsecutiveKills(int n) { this.consecutiveKills = n; }
    public long getLastKillTick() { return lastKillTick; }
    public void setLastKillTick(long t) { this.lastKillTick = t; }
    public int getLevelNumber() { return levelNumber; }
    public void setLevelNumber(int n) { this.levelNumber = n; }

    // ─── الگوهای امتیازی (Scored) ──────────────────────────────────────────────
    /**
     * ثبتِ یک اعلانِ الگوی میوپوینت + افزودنِ امتیاز.
     * <b>فقط در بازیِ امتیازی (SCORED)</b> فعال است — در مراحلِ عادی اعلانِ
     * میوپوینت نباید دیده شود (طبق درخواستِ کاربر).
     */
    public void pushMeoEvent(String label, long bonus) {
        if (level == null || level.getLevelType() != com.pvz2.model.enums.LevelType.SCORED) {
            return;
        }
        meoEvents.add(label);
        meoPoints += bonus;
    }
    /** برداشتِ اعلان‌های انباشته (پس از تحویل به گرافیک، پاک می‌شوند). */
    public List<String> drainMeoEvents() {
        if (meoEvents.isEmpty()) return java.util.Collections.emptyList();
        List<String> copy = new ArrayList<>(meoEvents);
        meoEvents.clear();
        return copy;
    }

    /**
     * صفِ توستِ «جمع‌آوری» — برای هر مرحله (برخلافِ meoEvents که فقط SCORED است).
     * مثلِ جمع‌آوریِ سکه/الماس/گلدان/غذای گیاه از زامبی‌های کشته‌شده.
     */
    private final List<String> collectEvents = new ArrayList<>();
    public void pushCollectEvent(String label) { collectEvents.add(label); }
    public List<String> drainCollectEvents() {
        if (collectEvents.isEmpty()) return java.util.Collections.emptyList();
        List<String> copy = new ArrayList<>(collectEvents);
        collectEvents.clear();
        return copy;
    }

    /**
     * صفِ «گردبادِ انداختنِ زامبی» (مصرِ باستان، موجِ نهایی). هر عنصر {col,row}
     * محلِ فرودِ گردباد است؛ لایه‌ی گرافیک انیمیشنِ intro→loop→outro را آنجا می‌زند.
     */
    private final List<int[]> tornadoDrops = new ArrayList<>();
    public void addTornadoDrop(int col, int row) { tornadoDrops.add(new int[]{col, row}); }
    public List<int[]> drainTornadoDrops() {
        if (tornadoDrops.isEmpty()) return java.util.Collections.emptyList();
        List<int[]> copy = new ArrayList<>(tornadoDrops);
        tornadoDrops.clear();
        return copy;
    }

    /**
     * خانه‌هایی که اژدهای فصلِ تاریک آتش زده — لایه‌ی گرافیک انیمیشنِ زمینِ سوخته
     * (SCORCHED_EARTH_TILE: animation→animation2→animation3) را آنجا پخش می‌کند.
     * هر عنصر {col1based, row1based}.
     */
    private final List<int[]> scorchedTiles = new ArrayList<>();
    public void addScorchedTile(int col, int row) { scorchedTiles.add(new int[]{col, row}); }
    public List<int[]> drainScorchedTiles() {
        if (scorchedTiles.isEmpty()) return java.util.Collections.emptyList();
        List<int[]> copy = new ArrayList<>(scorchedTiles);
        scorchedTiles.clear();
        return copy;
    }

    /**
     * چمن‌زنِ متحرک: پس از فعال‌شدن، از سرِ لاین حرکت می‌کند و هر زامبی که به آن
     * می‌رسد را می‌کشد (نه همه با هم). {@code x} ستونِ اعشاریِ ۱-based.
     */
    public static final class ActiveMower {
        public final int row;   // ۱-based
        public double x;         // ستونِ اعشاری؛ از لبه‌ی خانه (۰) شروع می‌شود
        public ActiveMower(int row) { this.row = row; this.x = 0.0; }
    }
    private final List<ActiveMower> activeMowers = new ArrayList<>();
    public List<ActiveMower> getActiveMowers() { return activeMowers; }
    public void launchMower(int row1based) { activeMowers.add(new ActiveMower(row1based)); }

    /** صفِ انفجارها (بمب گیلاسی/جالاپینو/دوم‌شروم): هر عنصر {col,row}. لرزش + جلوه. */
    private final List<int[]> explosionEvents = new ArrayList<>();
    public void addExplosion(int col, int row) { explosionEvents.add(new int[]{col, row}); }
    public List<int[]> drainExplosions() {
        if (explosionEvents.isEmpty()) return java.util.Collections.emptyList();
        List<int[]> copy = new ArrayList<>(explosionEvents);
        explosionEvents.clear();
        return copy;
    }

    /** صفِ لیزرهای تورکوایز: هر عنصر {row, nearCol, farCol}. گرافیک پرتوی محوشونده می‌کشد. */
    private final List<int[]> laserZaps = new ArrayList<>();
    public void addLaserZap(int row, int nearCol, int farCol) {
        laserZaps.add(new int[]{row, nearCol, farCol});
    }
    public List<int[]> drainLaserZaps() {
        if (laserZaps.isEmpty()) return java.util.Collections.emptyList();
        List<int[]> copy = new ArrayList<>(laserZaps);
        laserZaps.clear();
        return copy;
    }

    /**
     * صفِ پرتابه‌های اختاپوس: هر عنصر {srcCol,srcRow,tgtCol,tgtRow}. لایه‌ی گرافیک
     * انیمیشنِ ZOMBIE_OCTOPUS_PROJECTILE را روی این مسیر پخش می‌کند.
     */
    private final List<double[]> octopusTosses = new ArrayList<>();
    public void addOctopusToss(double srcCol, double srcRow, double tgtCol, double tgtRow) {
        octopusTosses.add(new double[]{srcCol, srcRow, tgtCol, tgtRow});
    }
    public List<double[]> drainOctopusTosses() {
        if (octopusTosses.isEmpty()) return java.util.Collections.emptyList();
        List<double[]> copy = new ArrayList<>(octopusTosses);
        octopusTosses.clear();
        return copy;
    }

    /** صفِ افکت‌های تمام‌صفحه (مثلِ "iceshroom" = بادِ یخیِ IceShroom). لایه‌ی گرافیک مصرف می‌کند. */
    private final List<String> screenEffects = new ArrayList<>();
    public void pushScreenEffect(String name) { screenEffects.add(name); }
    public List<String> drainScreenEffects() {
        if (screenEffects.isEmpty()) return java.util.Collections.emptyList();
        List<String> copy = new ArrayList<>(screenEffects);
        screenEffects.clear();
        return copy;
    }
    /** ثبتِ جمع‌آوریِ یک آیتم (خورشید) برای الگوی Item Collector. */
    public void registerItemCollected() {
        if (itemWindowStartTick < 0 || currentTick - itemWindowStartTick > 100) {
            itemWindowStartTick = currentTick;
            itemsInWindow = 1;
        } else {
            itemsInWindow++;
            if (itemsInWindow == 5) {
                pushMeoEvent("✨ ITEM COLLECTOR x5!", 200L * 5);
                itemWindowStartTick = -1;
                itemsInWindow = 0;
            }
        }
    }

    // Conveyor
    public List<PlantType> getConveyorQueue() { return conveyorQueue; }
    public int getLastConveyorTick() { return lastConveyorTick; }
    public void setLastConveyorTick(int t) { this.lastConveyorTick = t; }

    // Save Our Seeds
    public List<int[]> getProtectedPlantPositions() { return protectedPlantPositions; }

    // Timed War
    public int getTimedWarKillsAchieved() { return timedWarKillsAchieved; }
    public void setTimedWarKillsAchieved(int n) { this.timedWarKillsAchieved = n; }
    public int getTimedWarSunAchieved() { return timedWarSunAchieved; }
    public void setTimedWarSunAchieved(int n) { this.timedWarSunAchieved = n; }
    public boolean isTimedWarGoalReached() { return timedWarGoalReached; }
    public void setTimedWarGoalReached(boolean b) { this.timedWarGoalReached = b; }

    // Love Your Plants
    public List<int[]> getProtectedLovePlantPositions() { return protectedLovePlantPositions; }
    public void addProtectedLovePlantPosition(int x, int y) {
        protectedLovePlantPositions.add(new int[]{x, y});
    }
}
