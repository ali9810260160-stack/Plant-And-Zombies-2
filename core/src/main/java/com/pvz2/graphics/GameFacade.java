package com.pvz2.graphics;

import com.pvz2.exception.AuthException;
import com.pvz2.exception.GameException;
import com.pvz2.exception.ValidationException;
import com.pvz2.model.*;
import com.pvz2.model.enums.*;
import com.pvz2.model.plants.Plant;
import com.pvz2.model.plants.PlantDataRegistry;
import com.pvz2.model.plants.PlantStats;
import com.pvz2.model.tiles.Tile;
import com.pvz2.model.zombies.Zombie;
import com.pvz2.service.NewsService;

import java.util.*;

/**
 * پل ارتباطی بین لایه گرافیک (فاز ۲) و سرویس‌های بازی (فاز ۱).
 *
 * <p>هیچ منطق بازی اینجا پیاده‌سازی نمی‌شود — فقط delegation.
 */
public final class GameFacade {

    private static final GameFacade INSTANCE = new GameFacade();
    private GameFacade() {}
    public static GameFacade get() { return INSTANCE; }

    private ServiceLocator svc() { return ServiceLocator.getInstance(); }
    private AppState       app() { return AppState.getInstance(); }
    private GameSession    session() { return app().getCurrentSession(); }

    // =========================================================================
    //  Authentication
    // =========================================================================

    public String register(String username, String password, String confirm,
                           String nickname, String email, String genderStr) {
        try {
            Gender g = "female".equalsIgnoreCase(genderStr) ? Gender.FEMALE : Gender.MALE;
            svc().getAuthService().register(username, password, confirm, nickname, email, g);
            return null;
        } catch (ValidationException | AuthException e) {
            return e.getMessage();
        }
    }

    public String setSecurityQuestion(int questionIndex, String answer, String confirm) {
        try {
            User user = app().getCurrentUser();
            if (user == null) return "کاربر یافت نشد";
            if (!answer.equals(confirm)) return "جواب‌ها مطابقت ندارند";
            SecurityQuestion[] qs = SecurityQuestion.values();
            if (questionIndex < 0 || questionIndex >= qs.length) return "سوال نامعتبر";
            user.setSecurityQuestion(qs[questionIndex]);
            user.setSecurityAnswerHash(com.pvz2.util.HashUtil.sha256(answer));
            svc().getUserRepository().save(user);
            return null;
        } catch (Exception e) { return e.getMessage(); }
    }

    public String login(String username, String password, boolean stayLoggedIn) {
        try {
            User user = svc().getAuthService().login(username, password, stayLoggedIn);
            if (user == null) return "نام کاربری یا رمز عبور اشتباه است";
            svc().getUserRepository().save(user);
            app().setCurrentUser(user);
            return null;
        } catch (AuthException | ValidationException e) { return e.getMessage(); }
    }

    public void logout() {
        User u = app().getCurrentUser();
        if (u != null) { u.setStayLoggedIn(false); svc().getUserRepository().save(u); }
        app().logout();
        app().setCurrentSession(null);
    }

    public String forgotPasswordStep1(String username, String email) {
        User u = svc().getUserRepository().findByUsername(username);
        if (u == null) return "نام کاربری یافت نشد";
        if (!u.getEmail().equals(email)) return "ایمیل مطابقت ندارد";
        return null;
    }

    public String getSecurityQuestion(String username) {
        User u = svc().getUserRepository().findByUsername(username);
        return (u != null && u.getSecurityQuestion() != null)
                ? u.getSecurityQuestion().toString() : null;
    }

    public String verifySecurityAnswer(String username, String answer) {
        User u = svc().getUserRepository().findByUsername(username);
        if (u == null) return "کاربر یافت نشد";
        if (!com.pvz2.util.HashUtil.sha256(answer).equals(u.getSecurityAnswerHash()))
            return "جواب اشتباه است";
        return null;
    }

    public String resetPassword(String username, String newPassword) {
        try {
            User u = svc().getUserRepository().findByUsername(username);
            if (u == null) return "کاربر یافت نشد";
            svc().getAuthService().validatePassword(newPassword);
            u.setPasswordHash(com.pvz2.util.HashUtil.sha256(newPassword));
            svc().getUserRepository().save(u);
            return null;
        } catch (ValidationException e) { return e.getMessage(); }
    }

    // =========================================================================
    //  Game Control
    // =========================================================================

    /**
     * شروع یک مرحله.
     *
     * <p><b>پیش‌نیاز:</b> در {@code GameController.java} متد
     * {@code buildLevel} را از {@code private} به {@code public} تغییر دهید.
     */
    public String startLevel(String chapterKey, int levelNumber,
                              List<PlantType> plants) {
        try {
            ChapterType chapter = ChapterType.valueOf(chapterKey.toUpperCase());

            // buildLevel در GameController فراخوانی می‌شود
            // پیش‌نیاز: private Level buildLevel(...) → public Level buildLevel(...)
            Level level = svc().getGameController().buildLevel(chapter, levelNumber);

            GameSession sess = svc().getGameService().createSession(level, plants);
            app().setCurrentSession(sess);
            return null;
        } catch (Exception e) {
            return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        }
    }

    /** پیشبرد زمان — هر ۱۰ تیک = ۱ ثانیه بازی */
    public void advanceTicks(int n) {
        GameSession sess = session();
        if (sess == null || !sess.isInProgress()) return;
        svc().getGameService().advanceTime(sess, n);
    }

    public String plantPlant(PlantType type, int phase1X, int phase1Y) {
        try { svc().getGameService().plantPlant(session(), type, phase1X, phase1Y); return null; }
        catch (GameException | ValidationException e) { return e.getMessage(); }
    }

    public String pluckPlant(int phase1X, int phase1Y) {
        try { svc().getGameService().pluckPlant(session(), phase1X, phase1Y); return null; }
        catch (GameException e) { return e.getMessage(); }
    }

    public String collectSun(int phase1X, int phase1Y) {
        try {
            int gained = svc().getSunService().collectSun(session(), phase1X, phase1Y);
            return gained >= 0 ? null : "آفتابی در این مکان نیست";
        } catch (Exception e) { return null; }
    }

    public String feedPlant(int phase1X, int phase1Y) {
        try { svc().getGameService().feedPlant(session(), phase1X, phase1Y); return null; }
        catch (GameException e) { return e.getMessage(); }
    }

    public void startZombieWaves() {
        if (session() != null) session().setWaveStarted(true);
    }

    // ─── Cheats ───────────────────────────────────────────────────────────────
    public void cheatAddSun(int amount)   { if (session() != null) session().addSun(amount); }
    public void cheatAddPlantFood()       { if (session() != null) session().addPlantFood(); }
    public void cheatRemoveCooldown()     { if (session() != null) session().setCooldownCheated(true); }
    public void cheatNuke()               { if (session() != null) svc().getGameService().releaseNuke(session()); }
    public void cheatAddCurrency(int amount, boolean isDiamond) {
        User u = app().getCurrentUser();
        if (u == null) return;
        if (isDiamond) u.setGems(u.getGems() + amount);
        else           u.setCoins(u.getCoins() + (long) amount);
        svc().getUserRepository().save(u);
    }

    // =========================================================================
    //  Snapshot
    // =========================================================================

    public GameStateSnapshot buildSnapshot() {
        GameStateSnapshot snap = new GameStateSnapshot();
        GameSession sess = session();
        if (sess == null) return snap;
        fillResources(snap, sess);
        fillWaveInfo(snap, sess);
        fillTiles(snap, sess);
        fillPlants(snap, sess);
        fillZombies(snap, sess);
        fillProjectiles(snap, sess);
        fillSuns(snap, sess);
        fillLawnMowers(snap, sess);
        fillStatus(snap, sess);
        return snap;
    }

    private void fillResources(GameStateSnapshot s, GameSession sess) {
        s.sunCount       = sess.getSunAmount();
        s.plantFoodCount = sess.getPlantFoodCount();
        User u = app().getCurrentUser();
        if (u != null) { s.coinCount = (int) u.getCoins(); s.gemCount = u.getGems(); }
        s.meoPoints      = sess.getMeoPoints();
        s.conveyorQueue  = new ArrayList<>(sess.getConveyorQueue());
        s.levelType      = sess.getLevel().getLevelType();
        s.waitingForPlayerStart = !sess.isWaveStarted()
                && sess.getLevel().getLevelType() == LevelType.PLANT_WHAT_YOU_GET;
        s.deadlineColumn    = sess.getLevel().getDeadLineColumn();
        s.maxPlantsAllowed  = sess.getLevel().getMaxPlantsLost();
        s.plantsLost        = sess.getPlantsLost();
        s.timedWarKills     = sess.getTimedWarKillsAchieved();
        s.timedWarSun       = sess.getTimedWarSunAchieved();
        s.timedWarTarget    = sess.getLevel().getTimedWarZombieTarget();
        s.timedWarSunTarget = sess.getLevel().getTimedWarSunTarget();
        s.timedWarSeconds   = sess.getTimedWarRemainingSeconds();
        s.timedWarSunMode   = sess.getLevel().isTimedWarSunMode();
        s.egyptTornadoActive  = sess.isEgyptTornadoActive();
        s.frostbiteWindRows   = new ArrayList<>(sess.getFrostbiteWindAffectedRows());
    }

    private void fillWaveInfo(GameStateSnapshot s, GameSession sess) {
        s.currentWave = sess.getCurrentWaveIndex() + 1;
        s.totalWaves  = sess.getWaves() != null ? sess.getWaves().size() : 0;
        s.zombieProgress = computeZombieProgress(sess);
    }

    private float computeZombieProgress(GameSession sess) {
        List<Zombie> zombies = sess.getActiveZombies();
        if (zombies == null || zombies.isEmpty()) return 0f;
        double cols = sess.getGameMap().getCols();
        double minX = zombies.stream().mapToDouble(Zombie::getX).min().orElse(cols);
        return (float) Math.max(0, Math.min(1, (cols - minX) / (cols - 1)));
    }

    private void fillTiles(GameStateSnapshot s, GameSession sess) {
        GameMap map = sess.getGameMap();
        s.tiles = new GameStateSnapshot.TileType[map.getCols()][map.getRows()];
        for (int c = 1; c <= map.getCols(); c++) {
            for (int r = 1; r <= map.getRows(); r++) {
                Tile t = map.getTile(c, r);
                s.tiles[c - 1][r - 1] = mapTileType(t != null ? t.getType() : null);
            }
        }
    }

    private void fillPlants(GameStateSnapshot s, GameSession sess) {
        GameMap map = sess.getGameMap();
        Map<String, Plant> catPlants = sess.getCatPlants();
        for (int c = 1; c <= map.getCols(); c++) {
            for (int r = 1; r <= map.getRows(); r++) {
                Tile tile = map.getTile(c, r);
                if (tile == null) continue;
                if (tile.getPlant() != null)
                    s.plants.add(buildPlantInfo(tile.getPlant(), c, r, catPlants));
                if (tile.getSecondLayerPlant() != null) {
                    GameStateSnapshot.PlantInfo over =
                            buildPlantInfo(tile.getSecondLayerPlant(), c, r, catPlants);
                    over.isOverlay = true;
                    s.plants.add(over);
                }
            }
        }
    }

    private GameStateSnapshot.PlantInfo buildPlantInfo(Plant p, int col, int row,
                                                        Map<String, Plant> catPlants) {
        GameStateSnapshot.PlantInfo info = new GameStateSnapshot.PlantInfo();
        info.type        = p.getType().name().toLowerCase();
        info.phase1X     = col;
        info.phase1Y     = row;
        info.hp          = p.getCurrentHealth();
        info.maxHp       = p.getMaxHealth();
        info.freezeLevel = p.getFreezeLevel();
        info.boosted     = p.isBoosted();
        String key       = col + "," + row;
        info.isCat       = catPlants != null && catPlants.containsKey(key);
        double recharge  = p.getRechargeTime() * GameConstants.TICKS_PER_SECOND;
        info.cooldownFraction = recharge > 0
                ? (float)(p.getRemainingCooldownTicks() / recharge) : 0f;
        return info;
    }

    private void fillZombies(GameStateSnapshot s, GameSession sess) {
        for (Zombie z : sess.getActiveZombies()) {
            if (!z.isAlive()) continue;
            GameStateSnapshot.ZombieInfo info = new GameStateSnapshot.ZombieInfo();
            info.type          = z.getType().name().toLowerCase();
            info.phase1X       = z.getX();
            info.phase1Y       = z.getY();
            info.hp            = z.getCurrentHealth();
            info.maxHp         = z.getMaxHealth();
            info.isHypnotized  = z.isHypnotized();
            info.isGlowing     = z.isGlowing();
            info.movingBackward= z.isMovingBackward();
            info.isAttacking   = z.isAttacking();
            for (Map.Entry<ArmorType, Integer> e : z.getArmors().entrySet()) {
                GameStateSnapshot.ZombieInfo.ArmorInfo a =
                        new GameStateSnapshot.ZombieInfo.ArmorInfo();
                a.type  = e.getKey().name().toLowerCase();
                a.hp    = e.getValue();
                a.maxHp = getArmorMaxHp(e.getKey());
                info.armors.add(a);
            }
            for (ZombieEffect eff : z.getActiveEffects().keySet())
                info.effects.add(eff.name().toLowerCase());
            info.currentClip = z.isAttacking() ? "eating" : "walk";
            s.zombies.add(info);
        }
    }

    private int getArmorMaxHp(ArmorType t) {
        switch (t) {
            case CONE:          return 370;
            case BUCKET:        return 1100;
            case HELMET:        return 1600;
            case SHOULDER_ARMOR:return 1600;
            case BLOCK:         return 2200;
            default:            return 500;
        }
    }

    private void fillProjectiles(GameStateSnapshot s, GameSession sess) {
        for (com.pvz2.model.Projectile pr : sess.getActiveProjectiles()) {
            GameStateSnapshot.ProjectileInfo info = new GameStateSnapshot.ProjectileInfo();
            info.type          = pr.getType().name().toLowerCase();
            info.phase1X       = pr.getX();
            info.phase1Y       = pr.getY();
            info.movingRight   = pr.isMovingRight();
            info.isArc         = pr.isArc();
            info.targetPhase1X = pr.getTargetX();
            info.targetPhase1Y = pr.getTargetY();
            s.projectiles.add(info);
        }
    }

    private void fillSuns(GameStateSnapshot s, GameSession sess) {
        for (Sun sun : sess.getActiveSuns()) {
            if (sun.isCollected()) continue;
            GameStateSnapshot.SunInfo info = new GameStateSnapshot.SunInfo();
            info.phase1X      = sun.getX();
            info.phase1Y      = sun.getY();
            info.value        = sun.getValue();
            info.sunType      = sun.getType().name().toLowerCase();
            info.isLanded     = sun.isLanded();
            info.fallProgress = (float) sun.getFallProgress();
            s.sunItems.add(info);
        }
    }

    private void fillLawnMowers(GameStateSnapshot s, GameSession sess) {
        boolean[] mowers = sess.getGameMap().getLawnMowers();
        s.lawnMowerActive = mowers != null
                ? Arrays.copyOf(mowers, mowers.length)
                : new boolean[GameConstants.TILE_ROWS];
    }

    private void fillStatus(GameStateSnapshot s, GameSession sess) {
        switch (sess.getResult()) {
            case WIN:  s.status = GameStateSnapshot.GameStatus.WON;  break;
            case LOSE: s.status = GameStateSnapshot.GameStatus.LOST; break;
            default:   s.status = GameStateSnapshot.GameStatus.PLAYING;
        }
    }

    private GameStateSnapshot.TileType mapTileType(TileType t) {
        if (t == null) return GameStateSnapshot.TileType.NORMAL;
        switch (t) {
            case TOMBSTONE:
            case DARK_TOMBSTONE:   return GameStateSnapshot.TileType.TOMBSTONE;
            case ICY_GROUND:       return GameStateSnapshot.TileType.ICY_GROUND;
            case SLIPPERY_UP:      return GameStateSnapshot.TileType.SLIPPERY_UP;
            case SLIPPERY_DOWN:    return GameStateSnapshot.TileType.SLIPPERY_DOWN;
            case WATER:            return GameStateSnapshot.TileType.WATER;
            case LOW_TIDE:         return GameStateSnapshot.TileType.LOW_SHORE;
            case NECROMANCY:       return GameStateSnapshot.TileType.NECROMANCY;
            default:               return GameStateSnapshot.TileType.NORMAL;
        }
    }

    // =========================================================================
    //  User / Profile
    // =========================================================================

    public String getCurrentUsername() {
        User u = app().getCurrentUser(); return u != null ? u.getUsername() : "";
    }
    public String getCurrentNickname() {
        User u = app().getCurrentUser(); return u != null ? u.getNickname() : "";
    }
    public User getCurrentUser() { return app().getCurrentUser(); }

    public String changeUsername(String v) {
        try { svc().getUserService().changeUsername(app().getCurrentUser(), v); return null; }
        catch (Exception e) { return e.getMessage(); }
    }
    public String changeNickname(String v) {
        try { svc().getUserService().changeNickname(app().getCurrentUser(), v); return null; }
        catch (Exception e) { return e.getMessage(); }
    }
    public String changeEmail(String v) {
        try { svc().getUserService().changeEmail(app().getCurrentUser(), v); return null; }
        catch (Exception e) { return e.getMessage(); }
    }
    public String changePassword(String old, String nw) {
        try { svc().getUserService().changePassword(app().getCurrentUser(), old, nw); return null; }
        catch (Exception e) { return e.getMessage(); }
    }

    public boolean isLevelUnlocked(String chapter, int level) {
        User u = app().getCurrentUser();
        if (u == null) return level == 1;
        return svc().getLevelProgressService()
                .isLevelUnlocked(u, ChapterType.valueOf(chapter.toUpperCase()), level);
    }

    public Map<String, String> getProfileStats() {
        Map<String, String> m = new LinkedHashMap<>();
        User u = app().getCurrentUser();
        if (u == null) return m;
        m.put("نام کاربری",      u.getUsername());
        m.put("نام مستعار",      u.getNickname());
        m.put("بازی‌ها",         String.valueOf(u.getGamesPlayed()));
        m.put("سکه",            u.getCoins() + " 🪙");
        m.put("الماس",          u.getGems()  + " 💎");
        m.put("مراحل تکمیل",    String.valueOf(u.getLevelsCompleted()));
        m.put("بهترین میوپوینت", String.valueOf(u.getHighestMeoPoint()));
        return m;
    }

    public List<PlantEntry> getAllPlants() {
        List<PlantEntry> list = new ArrayList<>();
        User u = app().getCurrentUser();
        for (PlantType type : PlantType.values()) {
            PlantStats stats = PlantDataRegistry.getInstance().getStats(type);
            if (stats == null) continue;
            boolean unlocked = u != null && u.getUnlockedPlants() != null
                    && u.getUnlockedPlants().contains(type.name());
            list.add(new PlantEntry(type, stats, unlocked));
        }
        return list;
    }

    public List<String> getSeenZombies() {
        User u = app().getCurrentUser();
        if (u == null || u.getSeenZombies() == null) return Collections.emptyList();
        return u.getSeenZombies();
    }

    public List<LeaderboardEntry> getLeaderboard(String sortKey, boolean ascending) {
        List<User> users = svc().getUserRepository().getAllUsers();
        List<LeaderboardEntry> result = new ArrayList<>();
        for (User u : users) {
            result.add(new LeaderboardEntry(
                u.getUsername(), u.getNickname(),
                u.getLastReachedLevel() != null ? u.getLastReachedLevel() : "-",
                u.getMinigamesCompleted(), u.getDailyQuestsCompleted(),
                u.getRegularQuestsCompleted(), (int) u.getHighestMeoPoint()
            ));
        }
        result.sort(buildComparator(sortKey, ascending));
        return result;
    }

    // ─── News convenience ──────────────────────────────────────────────────────
    public NewsService news() { return svc().getNewsService(); }

    private Comparator<LeaderboardEntry> buildComparator(String key, boolean asc) {
        Comparator<LeaderboardEntry> cmp;
        switch (key) {
            case "minigame": cmp = Comparator.comparingInt(e -> e.minigameCount); break;
            case "daily":    cmp = Comparator.comparingInt(e -> e.dailyQuestCount); break;
            case "quest":    cmp = Comparator.comparingInt(e -> e.questCount); break;
            case "meopoint": cmp = Comparator.comparingInt(e -> e.bestMeopoint); break;
            default:         cmp = Comparator.comparing(e -> e.username); break;
        }
        return asc ? cmp : cmp.reversed();
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────
    public static class PlantEntry {
        public PlantType type; public PlantStats stats; public boolean unlocked;
        PlantEntry(PlantType t, PlantStats s, boolean u) { type=t; stats=s; unlocked=u; }
    }

    public static class LeaderboardEntry {
        public String username, nickname, lastLevel;
        public int minigameCount, dailyQuestCount, questCount, bestMeopoint;
        LeaderboardEntry(String u,String n,String l,int m,int d,int q,int mp){
            username=u;nickname=n;lastLevel=l;
            minigameCount=m;dailyQuestCount=d;questCount=q;bestMeopoint=mp;
        }
    }
}
