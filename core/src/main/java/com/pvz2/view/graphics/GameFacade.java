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
import com.pvz2.graphics.net.NetClient;
import com.pvz2.graphics.net.NetException;
import com.pvz2.graphics.net.NetLog;
import com.pvz2.graphics.net.UserCodec;
import com.pvz2.shared.protocol.MessageType;
import com.pvz2.shared.protocol.Packet;
import com.pvz2.shared.protocol.Wire;
import com.pvz2.shared.protocol.payload.AuthResponse;
import com.pvz2.shared.protocol.payload.ErrorPayload;
import com.pvz2.shared.protocol.payload.LoginRequest;
import com.pvz2.shared.protocol.payload.RecoverRequest;
import com.pvz2.shared.protocol.payload.RecoverResponse;
import com.pvz2.shared.protocol.payload.RegisterRequest;
import com.pvz2.shared.protocol.payload.ResetPasswordRequest;
import com.pvz2.shared.protocol.payload.SetSecurityRequest;
import com.pvz2.shared.protocol.payload.TokenRequest;

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
    //  Authentication  (phase 3: online-first with local fallback)
    // =========================================================================

    private NetClient net() { return NetClient.get(); }
    /** True when a live server connection exists (route auth through the server). */
    private boolean online() { return NetClient.get().isConnected(); }

    /** Human message from an {@code ERROR_RES} reply. */
    private String errMsg(Packet resp) {
        try {
            ErrorPayload ep = resp.payload(Wire.GSON, ErrorPayload.class);
            if (ep != null && ep.message != null) return ep.message;
        } catch (Exception ignored) { }
        return "Server error.";
    }

    // Cached during an online password-recovery flow (see forgotPasswordStep1).
    private String recoverQuestion;
    private String recoverAnswer;

    public String register(String username, String password, String confirm,
                           String nickname, String email, String genderStr) {
        if (online()) {
            try {
                RegisterRequest r = new RegisterRequest();
                r.username = username; r.password = password; r.confirmPassword = confirm;
                r.nickname = nickname; r.email = email;
                r.gender = "female".equalsIgnoreCase(genderStr) ? "FEMALE" : "MALE";
                r.stayLoggedIn = false;
                Packet resp = net().requestSync(MessageType.REGISTER_REQ, r);
                if (MessageType.REGISTER_RES.equals(resp.type)) {
                    AuthResponse ar = resp.payload(Wire.GSON, AuthResponse.class);
                    net().setToken(ar.token);      // account created; not logged in yet
                    return null;
                }
                return errMsg(resp);
            } catch (NetException e) {
                NetLog.info("register: server unreachable, using local: " + e.getMessage());
            }
        }
        try {
            Gender g = "female".equalsIgnoreCase(genderStr) ? Gender.FEMALE : Gender.MALE;
            svc().getAuthService().register(username, password, confirm, nickname, email, g);
            return null;
        } catch (ValidationException | AuthException e) {
            return e.getMessage();
        }
    }

    public String setSecurityQuestion(int questionIndex, String answer, String confirm) {
        SecurityQuestion[] qs = SecurityQuestion.values();
        if (questionIndex < 0 || questionIndex >= qs.length) return "Invalid question.";
        if (!answer.equals(confirm)) return "Answers do not match.";
        if (online() && net().token() != null) {
            try {
                SetSecurityRequest r = new SetSecurityRequest();
                r.token = net().token();
                r.question = qs[questionIndex].name();
                r.answer = answer; r.confirmAnswer = confirm;
                Packet resp = net().requestSync(MessageType.SET_SECURITY_REQ, r);
                if (MessageType.SET_SECURITY_RES.equals(resp.type)) return null;
                return errMsg(resp);
            } catch (NetException e) {
                NetLog.info("setSecurity: server unreachable, using local: " + e.getMessage());
            }
        }
        try {
            User user = app().getCurrentUser();
            if (user == null) return "User not found.";
            user.setSecurityQuestion(qs[questionIndex]);
            user.setSecurityAnswerHash(com.pvz2.util.HashUtil.sha256(answer));
            svc().getUserRepository().save(user);
            return null;
        } catch (Exception e) { return e.getMessage(); }
    }

    public String login(String username, String password, boolean stayLoggedIn) {
        if (online()) {
            try {
                LoginRequest r = new LoginRequest();
                r.username = username; r.password = password; r.stayLoggedIn = stayLoggedIn;
                Packet resp = net().requestSync(MessageType.LOGIN_REQ, r);
                if (MessageType.LOGIN_RES.equals(resp.type)) {
                    AuthResponse ar = resp.payload(Wire.GSON, AuthResponse.class);
                    applyAuthResponse(ar, stayLoggedIn);
                    return null;
                }
                return errMsg(resp);
            } catch (NetException e) {
                NetLog.info("login: server unreachable, using local: " + e.getMessage());
            }
        }
        try {
            User user = svc().getAuthService().login(username, password, stayLoggedIn);
            if (user == null) return "Incorrect username or password.";
            svc().getUserRepository().save(user);
            app().setCurrentUser(user);
            svc().getQuestService().loadForUser(user);
            com.pvz2.PVZApplication.applyUserSettings(user);
            return null;
        } catch (AuthException | ValidationException e) { return e.getMessage(); }
    }

    /**
     * Apply an authenticated server reply: store tokens, rebuild the {@link User}
     * from the server document, set it current, load quests + settings, and cache
     * locally. Persists the remember-me token when the user opted to stay in.
     */
    private void applyAuthResponse(AuthResponse ar, boolean stayLoggedIn) {
        net().setToken(ar.token);
        User user = UserCodec.toUser(svc().getUserRepository(), ar.user);
        svc().getUserRepository().save(user); // local cache for offline fallback
        app().setCurrentUser(user);
        svc().getQuestService().loadForUser(user);
        com.pvz2.PVZApplication.applyUserSettings(user);
        if (stayLoggedIn && ar.rememberToken != null) {
            com.pvz2.graphics.net.SessionStore.save(user.getUsername(), ar.rememberToken);
        } else if (!stayLoggedIn) {
            com.pvz2.graphics.net.SessionStore.clear();
        }
    }

    /**
     * Attempt seamless online auto-login using the persisted remember-me token.
     * Returns true if the server accepted it and a user is now logged in.
     */
    public boolean tryResumeOnline() {
        if (!online()) return false;
        com.pvz2.graphics.net.SessionStore.Saved saved =
                com.pvz2.graphics.net.SessionStore.load();
        if (saved == null) return false;
        try {
            com.pvz2.shared.protocol.payload.ResumeRequest r =
                    new com.pvz2.shared.protocol.payload.ResumeRequest();
            r.token = saved.rememberToken;
            Packet resp = net().requestSync(MessageType.RESUME_REQ, r);
            if (MessageType.RESUME_RES.equals(resp.type)) {
                AuthResponse ar = resp.payload(Wire.GSON, AuthResponse.class);
                applyAuthResponse(ar, true);
                return true;
            }
            com.pvz2.graphics.net.SessionStore.clear(); // token no longer valid
        } catch (NetException e) {
            NetLog.info("resume online failed: " + e.getMessage());
        }
        return false;
    }

    public void logout() {
        if (online() && net().token() != null) {
            try { net().requestSync(MessageType.LOGOUT_REQ, new TokenRequest(net().token())); }
            catch (NetException ignored) { }
            net().setToken(null);
        }
        com.pvz2.graphics.net.SessionStore.clear();
        User u = app().getCurrentUser();
        if (u != null) { u.setStayLoggedIn(false); svc().getUserRepository().save(u); }
        app().logout();
        app().setCurrentSession(null);
    }

    public String forgotPasswordStep1(String username, String email) {
        recoverQuestion = null; recoverAnswer = null;
        if (online()) {
            try {
                RecoverRequest r = new RecoverRequest();
                r.username = username; r.email = email;
                Packet resp = net().requestSync(MessageType.RECOVER_REQ, r);
                if (MessageType.RECOVER_RES.equals(resp.type)) {
                    RecoverResponse rr = resp.payload(Wire.GSON, RecoverResponse.class);
                    recoverQuestion = rr != null ? rr.securityQuestion : null;
                    return null;
                }
                return errMsg(resp);
            } catch (NetException e) {
                NetLog.info("recover: server unreachable, using local: " + e.getMessage());
            }
        }
        User u = svc().getUserRepository().findByUsername(username);
        if (u == null) return "Username not found.";
        if (!u.getEmail().equals(email)) return "Email does not match.";
        return null;
    }

    public String getSecurityQuestion(String username) {
        if (online() && recoverQuestion != null) {
            try { return SecurityQuestion.valueOf(recoverQuestion).toString(); }
            catch (IllegalArgumentException e) { return recoverQuestion; }
        }
        User u = svc().getUserRepository().findByUsername(username);
        return (u != null && u.getSecurityQuestion() != null)
                ? u.getSecurityQuestion().toString() : null;
    }

    public String verifySecurityAnswer(String username, String answer) {
        if (online()) {
            // The server verifies the answer atomically at reset time; cache it here.
            recoverAnswer = answer;
            return null;
        }
        User u = svc().getUserRepository().findByUsername(username);
        if (u == null) return "User not found.";
        if (!com.pvz2.util.HashUtil.sha256(answer).equals(u.getSecurityAnswerHash()))
            return "Incorrect answer.";
        return null;
    }

    public String resetPassword(String username, String newPassword) {
        if (online()) {
            try {
                ResetPasswordRequest r = new ResetPasswordRequest();
                r.username = username; r.answer = recoverAnswer; r.newPassword = newPassword;
                Packet resp = net().requestSync(MessageType.RESET_PW_REQ, r);
                if (MessageType.RESET_PW_RES.equals(resp.type)) { recoverAnswer = null; return null; }
                return errMsg(resp);
            } catch (NetException e) {
                NetLog.info("resetPw: server unreachable, using local: " + e.getMessage());
            }
        }
        try {
            User u = svc().getUserRepository().findByUsername(username);
            if (u == null) return "User not found.";
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

    /**
     * شروع یک مینی‌گیم با نوع مرحله‌ی اجباری (VASEBREAKER/WALLNUT_BOWLING/I_ZOMBIE).
     * مینی‌گیم‌ها در جدول adventure ثبت نشده‌اند، پس نوعشان صریح پاس داده می‌شود.
     */
    public String startMinigame(String chapterKey, int levelNumber,
                                LevelType type, List<PlantType> plants) {
        try {
            ChapterType chapter = ChapterType.valueOf(chapterKey.toUpperCase());
            Level level = svc().getGameController().buildLevel(chapter, levelNumber, type);
            GameSession sess = svc().getGameService().createSession(level, plants);
            sess.setLevelNumber(levelNumber);
            app().setCurrentSession(sess);
            return null;
        } catch (Exception e) {
            return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        }
    }

    /** شکستن یک کوزه در مرحله‌ی کوزه‌شکنی (کلیک روی کوزه). */
    public String breakVase(int col, int row) {
        try { svc().getGameService().breakVase(session(), col, row); return null; }
        catch (GameException | ValidationException e) { return e.getMessage(); }
    }

    /** کاشتِ یک زامبی در مرحله‌ی من‌زامبی (کلیک روی خانه‌ی سمت راست). */
    public String placeZombie(ZombieType type, int col, int row) {
        try { svc().getGameService().placeZombie(session(), type, col, row); return null; }
        catch (GameException | ValidationException e) { return e.getMessage(); }
    }

    /** جابه‌جاییِ دو گیاهِ مجاور در مرحله‌ی Beghouled (فقط اگر ترکیب بسازد). */
    public String beghouledSwap(int col1, int row1, int col2, int row2) {
        try { svc().getGameService().beghouledSwap(session(), col1, row1, col2, row2); return null; }
        catch (GameException | ValidationException e) { return e.getMessage(); }
    }

    /** ارتقای همه‌ی گیاهانِ نوعِ from به to با خرجِ خورشید (Beghouled). */
    public String beghouledUpgrade(com.pvz2.model.enums.PlantType from,
                                   com.pvz2.model.enums.PlantType to) {
        try { svc().getGameService().beghouledUpgrade(session(), from, to); return null; }
        catch (GameException | ValidationException e) { return e.getMessage(); }
    }

    /**
     * مرحله جاری (اگر جلسه فعالی وجود داشته باشد).
     *
     * <p>مصرف اصلی: {@code GameRenderer} این را برای بارگذاری لایه‌های صحیح
     * نقشه Tiled (فصل/مینی‌گیم/special) از طریق
     * {@code LevelMapConfig.fromLevel(...)} استفاده می‌کند.
     */
    public Level getCurrentLevel() {
        GameSession sess = session();
        return sess != null ? sess.getLevel() : null;
    }

    /**
     * جلسه بازی خام فعلی — برای لایه انیمیشن (بخش سوم) که مستقیماً روی
     * موجودیت‌های زنده (Plant/Zombie/Projectile/Sun) کار می‌کند، نه DTO.
     */
    public GameSession getCurrentSession() {
        return session();
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

    /**
     * ذخیره‌ی نتیجه‌ی بازی امتیازی. آنلاین → امتیاز به سرور ارسال می‌شود و
     * «My Point» فقط اگر رکورد جدید باشد به‌روزرسانی می‌شود (رقابتِ سراسری).
     * آفلاین → همان به‌روزرسانیِ محلیِ highestMeoPoint.
     */
    public void saveScoredResult() {
        GameSession sess = session();
        User u = app().getCurrentUser();
        if (sess == null || u == null) return;
        long score = sess.getMeoPoints();
        if (online() && net().token() != null) {
            try {
                Packet resp = net().requestSync(MessageType.SUBMIT_SCORE_REQ,
                        new com.pvz2.shared.protocol.payload.SubmitScoreRequest(net().token(), score));
                if (MessageType.SUBMIT_SCORE_RES.equals(resp.type)) {
                    com.pvz2.shared.protocol.payload.SubmitScoreResponse sr =
                            resp.payload(Wire.GSON, com.pvz2.shared.protocol.payload.SubmitScoreResponse.class);
                    if (sr != null) u.setHighestMeoPoint(sr.myPoint); // mirror My Point locally
                    return;
                }
            } catch (NetException e) {
                NetLog.info("submitScore: server unreachable, using local: " + e.getMessage());
            }
        }
        if (score > u.getHighestMeoPoint()) u.setHighestMeoPoint(score);
        svc().getUserRepository().save(u);
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
        fillSeedCooldowns(snap, sess);
        fillLawnMowers(snap, sess);
        fillMinigame(snap, sess);
        fillBoss(snap, sess);
        fillStatus(snap, sess);
        return snap;
    }

    /** داده‌های مخصوص مینی‌گیم (کوزه‌شکنی / بولینگ) را به snapshot اضافه می‌کند. */
    private void fillMinigame(GameStateSnapshot s, GameSession sess) {
        LevelType lt = sess.getLevel().getLevelType();
        Object ms = sess.getMinigameState();
        if (lt == LevelType.VASEBREAKER && ms instanceof VasebreakerState) {
            VasebreakerState vb = (VasebreakerState) ms;
            for (VasebreakerState.Vase v : vb.getVases()) {
                if (v.broken) continue;
                GameStateSnapshot.VaseInfo vi = new GameStateSnapshot.VaseInfo();
                vi.col = v.col;
                vi.row = v.row;
                vi.kind = v.kind.name().toLowerCase();
                s.vases.add(vi);
            }
            s.vasesRemaining = vb.remainingVaseCount();
            // نوار بذرِ رایگان از انبارِ کوزه‌شکنی پر می‌شود (بازاستفاده از مسیر conveyor).
            s.conveyorQueue = vb.flattenedInventory();
        } else if (lt == LevelType.WALLNUT_BOWLING && ms instanceof BowlingState) {
            BowlingState bw = (BowlingState) ms;
            for (BowlingState.Ball b : bw.getBalls()) {
                GameStateSnapshot.BowlingBallInfo bi = new GameStateSnapshot.BowlingBallInfo();
                bi.x = b.x;
                bi.y = b.y;
                bi.type = b.type.name().toLowerCase();
                s.bowlingBalls.add(bi);
            }
        } else if (lt == LevelType.I_ZOMBIE && ms instanceof IZombieState) {
            IZombieState iz = (IZombieState) ms;
            s.izombieBrains = Arrays.copyOf(iz.getBrains(), iz.getBrains().length);
            int tick = sess.getCurrentTick();
            int sun  = sess.getSunAmount();
            for (IZombieState.Placeable p : iz.getRoster()) {
                GameStateSnapshot.ZombieCardInfo ci = new GameStateSnapshot.ZombieCardInfo();
                ci.type = p.type.name().toLowerCase();
                ci.cost = p.cost;
                ci.cooldownFraction = iz.cooldownFraction(p.type, tick);
                ci.affordable = sun >= p.cost && iz.isReady(p.type, tick);
                s.izombieRoster.add(ci);
            }
        } else if (lt == LevelType.VERSUS && ms instanceof IZombieState) {
            // دونفره: مغزها + roster زامبی (هزینه/آماده‌بودن با خورشیدِ زامبی) +
            // خورشیدِ زامبی + تایمر + برنده — برای HUD و نوارِ زامبیِ مهمان.
            IZombieState iz = (IZombieState) ms;
            s.izombieBrains = Arrays.copyOf(iz.getBrains(), iz.getBrains().length);
            int tick = sess.getCurrentTick();
            int zsun = sess.getZombieSun();
            for (IZombieState.Placeable p : iz.getRoster()) {
                GameStateSnapshot.ZombieCardInfo ci = new GameStateSnapshot.ZombieCardInfo();
                ci.type = p.type.name().toLowerCase();
                ci.cost = p.cost;
                ci.cooldownFraction = iz.cooldownFraction(p.type, tick);
                ci.affordable = zsun >= p.cost && iz.isReady(p.type, tick);
                s.izombieRoster.add(ci);
            }
            s.versusZombieSun    = zsun;
            s.versusSecondsLeft  = Math.max(0, sess.getVersusTicksLeft() / 10);
            s.versusWinnerRole   = sess.getVersusWinnerRole() != null
                    ? sess.getVersusWinnerRole() : "";
        } else if (lt == LevelType.BEGHOULED && ms instanceof com.pvz2.model.BeghouledState) {
            com.pvz2.model.BeghouledState bg = (com.pvz2.model.BeghouledState) ms;
            s.beghouledActive  = true;
            s.beghouledMatches = bg.getMatchesMade();
            s.beghouledTarget  = bg.getTarget();
            // حفره‌ها (crater) برای رندر
            int rows = sess.getGameMap().getRows();
            int cols = sess.getGameMap().getCols();
            for (int c = 1; c <= cols && c <= GameConstants.TILE_COLS; c++) {
                for (int r = 1; r <= rows && r <= GameConstants.TILE_ROWS; r++) {
                    com.pvz2.model.tiles.Tile t = sess.getGameMap().getTile(c, r);
                    if (t != null && t.isCrater()) s.craters[c - 1][r - 1] = true;
                }
            }
        }
    }

    /** داده‌های مرحله‌ی رئیس (Zomboss) را به snapshot اضافه می‌کند. */
    private void fillBoss(GameStateSnapshot s, GameSession sess) {
        com.pvz2.model.Boss boss = sess.getBoss();
        if (boss == null) return;
        s.bossActive            = true;
        s.bossPhase1X           = boss.getX();
        s.bossLane              = boss.topRow(sess.getGameMap().getRows());
        s.bossLaneSpan          = boss.getLaneSpan();
        s.bossHealthFraction    = boss.healthFraction();
        s.bossSegmentsRemaining = boss.segmentsRemaining();
        s.bossSegmentFraction   = boss.currentSegmentFraction();
        s.bossPhase             = boss.phase();
        s.bossChapter           = boss.getChapter() != null
                ? boss.getChapter().name().toLowerCase() : "";
        s.bossState             = boss.getState().name().toLowerCase();
        s.bossAbility           = boss.getCurrentAbility() != null
                ? boss.getCurrentAbility().name().toLowerCase() : "";
        s.bossChargeOffset      = boss.getChargeOffset();
        s.bossStateTime         = (float) boss.getStateTime();
        s.bossDefeated          = boss.isDefeated();
        for (com.pvz2.model.Boss.BossAttack atk : boss.getAttacks()) {
            GameStateSnapshot.BossAttackInfo bi = new GameStateSnapshot.BossAttackInfo();
            bi.type        = atk.type.name().toLowerCase();
            bi.targetCol   = atk.targetCol;
            bi.targetRow   = atk.targetRow;
            bi.targetRow2  = atk.targetRow2;
            bi.wholeColumn = atk.wholeColumn;
            bi.phase       = atk.phase.name().toLowerCase();
            bi.progress    = atk.progress;
            s.bossAttacks.add(bi);
        }
    }

    private void fillResources(GameStateSnapshot s, GameSession sess) {
        s.sunCount       = sess.getSunAmount();
        s.plantFoodCount = sess.getPlantFoodCount();
        User u = app().getCurrentUser();
        if (u != null) { s.coinCount = (int) u.getCoins(); s.gemCount = u.getGems(); }
        s.meoPoints      = sess.getMeoPoints();
        s.meoEvents      = sess.drainMeoEvents();
        s.collectEvents  = sess.drainCollectEvents();
        s.tornadoDrops   = sess.drainTornadoDrops();
        s.screenEffects  = sess.drainScreenEffects();
        s.scorchedTiles  = sess.drainScorchedTiles();
        s.explosionEvents = sess.drainExplosions();
        s.octopusTosses  = sess.drainOctopusTosses();
        s.laserZaps      = sess.drainLaserZaps();
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
        // خانه‌های خطر SAVE_OUR_SEEDS — موقعیت دقیق گیاهان محافظت‌شده
        if (sess.getLevel().getLevelType() == LevelType.SAVE_OUR_SEEDS
                && sess.getProtectedPlantPositions() != null) {
            for (int[] pos : sess.getProtectedPlantPositions())
                s.protectedTiles.add(new int[]{ pos[0], pos[1] });
        }
    }

    private void fillWaveInfo(GameStateSnapshot s, GameSession sess) {
        s.currentWave = sess.getCurrentWaveIndex() + 1;
        s.totalWaves  = sess.getWaves() != null ? sess.getWaves().size() : 0;
        s.zombieProgress = computeZombieProgress(sess);
    }

    /**
     * پیشرفتِ کلیِ مرحله در طولِ همه‌ی موج‌ها (۰..۱) — برای نوارِ پیشرفتِ موج.
     *
     * <p>باگِ قبلی: پیشرفت بر اساسِ موقعیتِ جلوترین زامبی محاسبه می‌شد، پس همین که
     * اولین زامبی از راست تا خانه راه می‌رفت، نوار پر می‌شد (در ~۱۰ ثانیه‌ی اول).
     *
     * <p>درست: نوار باید متناسب با پیشرویِ واقعی در «زمان‌بندیِ موج‌ها» جلو برود.
     * موج‌ها بر اساسِ درصدِ جانِ ازدست‌رفته پیش می‌روند (موجِ بعد در ۷۵٪ شروع می‌شود
     * — {@link com.pvz2.model.Wave#shouldTriggerNextWave()}). پس:
     * <pre>progress = (waveIndex + within) / totalWaves</pre>
     * که {@code within} برای موج‌های غیرِنهایی نسبتِ [۰..۰٫۷۵]→[۰..۱] است تا در لحظه‌ی
     * شروعِ موجِ بعد دقیقاً به مرزِ نشانگرِ آن موج برسد (بدونِ پرش)، و برای موجِ نهایی
     * تا نابودیِ کاملِ آن ادامه می‌یابد.
     */
    private float computeZombieProgress(GameSession sess) {
        java.util.List<com.pvz2.model.Wave> waves = sess.getWaves();
        if (waves == null || waves.isEmpty()) {
            // بدونِ سیستمِ موج (مثلِ مینی‌گیم‌ها) — از پیشرویِ جلوترین زامبی استفاده کن.
            java.util.List<Zombie> zs = sess.getActiveZombies();
            if (zs == null || zs.isEmpty()) return 0f;
            double cols = sess.getGameMap().getCols();
            double minX = zs.stream().mapToDouble(Zombie::getX).min().orElse(cols);
            return (float) Math.max(0, Math.min(1, (cols - minX) / Math.max(1, cols - 1)));
        }
        int total = waves.size();
        int idx   = sess.getCurrentWaveIndex();          // ۰-based
        if (idx >= total) return 1f;

        com.pvz2.model.Wave current = sess.getCurrentWave();
        double within = 0;
        if (current != null && current.isStarted()) {
            double lost = Math.max(0, Math.min(1, current.getHealthLostPercent()));
            within = current.isFinalWave() ? lost : Math.min(1.0, lost / 0.75);
        }
        float progress = (float) ((idx + within) / total);
        return Math.max(0f, Math.min(1f, progress));
    }

    private void fillTiles(GameStateSnapshot s, GameSession sess) {
        GameMap map = sess.getGameMap();
        s.tiles = new GameStateSnapshot.TileType[map.getCols()][map.getRows()];
        s.tombstoneDamage = new float[map.getCols()][map.getRows()];
        s.tombstoneReward = new int[map.getCols()][map.getRows()];
        for (int c = 1; c <= map.getCols(); c++) {
            for (int r = 1; r <= map.getRows(); r++) {
                Tile t = map.getTile(c, r);
                s.tiles[c - 1][r - 1] = mapTileType(t != null ? t.getType() : null);
                // کسرِ آسیبِ سنگ‌قبر (HP اولیه ۷۰۰) — برای انتخابِ کلیپِ ترک‌خوردگی
                if (t != null && t.isTombstone()) {
                    s.tombstoneDamage[c - 1][r - 1] =
                            Math.max(0f, Math.min(1f, 1f - t.getTileHealth() / 700f));
                    switch (t.getReward()) {
                        case SUN:        s.tombstoneReward[c - 1][r - 1] = 1; break;
                        case PLANT_FOOD: s.tombstoneReward[c - 1][r - 1] = 2; break;
                        default:         s.tombstoneReward[c - 1][r - 1] = 0; break;
                    }
                }
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
        info.iceBlockStage = p.isFrozen() ? p.getIceMeltHits() : -1;
        info.boosted     = p.isBoosted();
        String key       = col + "," + row;
        info.isCat       = catPlants != null && catPlants.containsKey(key);
        double recharge  = p.getRechargeTime() * GameConstants.TICKS_PER_SECOND;
        info.cooldownFraction = recharge > 0
                ? (float)(p.getRemainingCooldownTicks() / recharge) : 0f;
        return info;
    }

    private void fillZombies(GameStateSnapshot s, GameSession sess) {
        User seenUser = app().getCurrentUser();
        for (Zombie z : sess.getActiveZombies()) {
            if (!z.isAlive()) continue;
            // سیوِ «زامبیِ دیده‌شده» — به محضِ دیده شدنِ یک نوعِ جدید برای این
            // کاربر سیو و یک خبر منتشر می‌شود (قبلاً هیچ‌جا صدا زده نمی‌شد).
            markZombieSeenIfNew(seenUser, z);
            GameStateSnapshot.ZombieInfo info = new GameStateSnapshot.ZombieInfo();
            info.type          = z.getType().name().toLowerCase();
            info.netId         = z.getNetId();
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
            info.iceBlockStage = z.hasEffect(ZombieEffect.FROZEN) ? z.getIceMeltHits() : -1;
            info.currentClip = z.isAttacking() ? "eating" : "walk";
            s.zombies.add(info);
        }
    }

    /**
     * اگر این نوع زامبی برای اولین بار دیده می‌شود: در {@code seenZombies} کاربر
     * سیو و یک خبر ({@code onZombieFirstSeen}) منتشر می‌کند. idempotent — بعد از
     * اولین بار دیگر چیزی سیو/منتشر نمی‌شود.
     */
    private void markZombieSeenIfNew(User user, Zombie z) {
        if (user == null || z == null) return;
        String ztype = z.getType().name();
        java.util.List<String> seen = user.getSeenZombies();
        if (seen != null && seen.contains(ztype)) return;
        svc().getUserService().markZombieSeen(user, ztype);
        try { svc().getNewsService().onZombieFirstSeen(user, ztype); } catch (Exception ignored) { }
    }

    private int getArmorMaxHp(ArmorType t) {
        switch (t) {
            case CONE:          return 370;
            case BUCKET:        return 1100;
            case HELMET:        return 1600;
            case SHOULDER_ARMOR:return 1600;
            case BLOCK:         return 2200;
            case PIANO:         return 1000;
            default:            return 500;
        }
    }

    private void fillProjectiles(GameStateSnapshot s, GameSession sess) {
        for (com.pvz2.model.Projectile pr : sess.getActiveProjectiles()) {
            GameStateSnapshot.ProjectileInfo info = new GameStateSnapshot.ProjectileInfo();
            info.type          = pr.getType().name().toLowerCase();
            info.phase1X       = pr.getX();
            info.startPhase1X  = pr.getStartX();
            info.phase1Y       = pr.getY();
            info.movingRight   = pr.isMovingRight();
            info.isArc         = pr.isArc();
            info.targetPhase1X = pr.getTargetX();
            info.targetPhase1Y = pr.getTargetY();
            s.projectiles.add(info);
        }
    }

    private void fillSeedCooldowns(GameStateSnapshot s, GameSession sess) {
        for (java.util.Map.Entry<com.pvz2.model.enums.PlantType, Integer> e
                : sess.getSeedCdRemaining().entrySet()) {
            if (e.getValue() != null && e.getValue() > 0) {
                s.seedCooldowns.put(e.getKey().name().toLowerCase(),
                        sess.seedCooldownFraction(e.getKey()));
            }
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
            info.beingStolen  = sun.isBeingStolen();
            info.stealProgress = (float) sun.getStealProgress();
            info.stealerCol   = sun.getStealerCol();
            info.stealerRow   = sun.getStealerRow();
            s.sunItems.add(info);
        }
    }

    private void fillLawnMowers(GameStateSnapshot s, GameSession sess) {
        boolean[] mowers = sess.getGameMap().getLawnMowers();
        s.lawnMowerActive = mowers != null
                ? Arrays.copyOf(mowers, mowers.length)
                : new boolean[GameConstants.TILE_ROWS];
        s.movingMowers = new ArrayList<>();
        for (GameSession.ActiveMower m : sess.getActiveMowers())
            s.movingMowers.add(new double[]{m.row, m.x});
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

    /** تعداد کل مراحل یک فصل (از {@link com.pvz2.service.LevelProgressService}). */
    public int getLevelsInChapter(String chapter) {
        return svc().getLevelProgressService()
                .getLevelsInChapter(ChapterType.valueOf(chapter.toUpperCase()));
    }

    /** نام نوع یک مرحله (Normal/Boss/نوع خاص فصل) — مستقیم از فاز یک. */
    public String getLevelTypeName(String chapter, int level) {
        return svc().getLevelProgressService()
                .getLevelTypeName(ChapterType.valueOf(chapter.toUpperCase()), level);
    }

    /** تعدادِ اسلاتِ بذرِ قفل‌شده در مرحله‌ی انتخاب گیاه (Locked Plants = ۲، بقیه ۰). */
    public int getLockedPlantSlots(String chapter, int level) {
        try {
            return svc().getLevelProgressService()
                    .getLockedSlotCount(ChapterType.valueOf(chapter.toUpperCase()), level);
        } catch (Exception e) {
            return 0;
        }
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
            int level = u != null ? u.getPlantUpgradeLevel(type) : 0;
            // یادداشت: پرچم دائمی «boosted» هنوز در User مدل نشده؛ فعلاً به‌عنوان
            // نماینده از «سطح ارتقای کامل» استفاده می‌شود — صفحه‌ی کلکسیون
            // (Phase 2) این مقدار را می‌خواند تا پس‌زمینه‌ی طلایی boost.png را
            // نشان دهد. اگر بعداً پرچم واقعی اضافه شد، اینجا جایگزین شود.
            boolean boosted = level >= 3;
            list.add(new PlantEntry(type, stats, unlocked, level, boosted));
        }
        return list;
    }

    //======================================================================
    //  Plant Upgrade / Boost economy (صفحه‌ی انتخاب گیاه — فاز ۲)
    // =========================================================================

    /** هزینه‌ی سکه برای ارتقای گیاه از سطح فعلی به سطح بعدی — سقف در سطح ۳. */
    public int getUpgradeCostCoins(PlantType type) {
        User u = app().getCurrentUser();
        int level = u != null ? u.getPlantUpgradeLevel(type) : 0;
        if (level >= 3) return 0;
        PlantStats stats = PlantDataRegistry.getInstance().getStats(type);
        int base = stats != null ? Math.max(50, stats.getSunCost()) : 100;
        // فرمول ساده و قابل‌تنظیم: هزینه با هر سطح افزایش می‌یابد (بدون داده‌ی
        // رسمی upgrade-cost در PlantStats). می‌تونی این فرمول رو با مقادیر
        // واقعی طراحی بازی جایگزین کنی.
        return base * 2 * (level + 1);
    }

    /** هزینه‌ی الماس برای boost کردن — مقدار ثابت (مطابق مرجع UI). */
    public int getBoostCostGems() { return GameConstants.PLANT_BOOST_COST_GEMS; }

    public boolean isPlantMaxLevel(PlantType type) {
        User u = app().getCurrentUser();
        return u != null && u.getPlantUpgradeLevel(type) >= 3;
    }

    /** ارتقای گیاه با کسر سکه — false اگر سکه کافی نبود یا سطح از قبل حداکثره. */
    public boolean upgradePlant(PlantType type) {
        User u = app().getCurrentUser();
        if (u == null || u.getPlantUpgradeLevel(type) >= 3) return false;
        int cost = getUpgradeCostCoins(type);
        if (u.getCoins() < cost) return false;
        u.setCoins(u.getCoins() - cost);
        int newLevel = u.incrementPlantUpgradeLevel(type.name());
        newsSafe(n -> n.onPlantUpgraded(u, type.name(), newLevel));
        return true;
    }

    /** فعال‌سازی boost با کسر الماس — false اگر الماس کافی نبود یا از قبل boost شده. */
    public boolean boostPlant(PlantType type) {
        User u = app().getCurrentUser();
        if (u == null || u.isPlantBoosted(type)) return false;
        int cost = getBoostCostGems();
        if (u.getGems() < cost) return false;
        u.setGems(u.getGems() - cost);
        u.setPlantBoosted(type, true);
        newsSafe(n -> n.onPlantBoosted(u, type.name()));
        return true;
    }

    //======================================================================
    //  Collection economy — خرید/ارتقای گیاه با اقتصادِ فاز ۱ (X3/Y3/Z3/T3)
    //  پورتِ وفادارِ CollectionController: خرید = سکه‌ی ثابت، ارتقا = سکه + کیسه‌بذر.
    // =========================================================================

    private static final long[] COLL_UPGRADE_COIN_COST = { 500L, 1500L, 3000L };
    private static final int[]  COLL_UPGRADE_SEED_COST  = { 5, 10, 20 };
    private static final int    COLL_MAX_UPGRADE_LEVEL  = 3;
    private static final int    PLANT_UNLOCK_COST_COINS = 2000;

    /** تعداد کیسه‌های بذرِ موجودِ کاربر برای این گیاه (نمایش در کلکسیون — T3). */
    public int getSeedPacketCount(PlantType type) {
        User u = app().getCurrentUser();
        return u != null ? u.getSeedPackets(type) : 0;
    }

    /** تعداد کیسه‌بذرِ لازم برای ارتقای بعدی (۰ اگر حداکثر سطح). */
    public int getUpgradeSeedCost(PlantType type) {
        User u = app().getCurrentUser();
        int level = u != null ? u.getPlantUpgradeLevel(type) : 0;
        if (level >= COLL_MAX_UPGRADE_LEVEL) return 0;
        return COLL_UPGRADE_SEED_COST[level];
    }

    /** هزینه‌ی سکه‌ی ارتقای بعدی طبق اقتصادِ فاز ۱ کلکسیون (۰ اگر حداکثر سطح). */
    public int getUpgradeCoinCostCollection(PlantType type) {
        User u = app().getCurrentUser();
        int level = u != null ? u.getPlantUpgradeLevel(type) : 0;
        if (level >= COLL_MAX_UPGRADE_LEVEL) return 0;
        return (int) COLL_UPGRADE_COIN_COST[level];
    }

    /** هزینه‌ی سکه‌ی خریدِ گیاهِ قفل. */
    public int getPlantUnlockCostCoins() { return PLANT_UNLOCK_COST_COINS; }

    /** خرید گیاهِ قفل — {@code null} یعنی موفق، وگرنه پیامِ خطا (برای toast). */
    public String tryBuyPlant(PlantType type) {
        User u = app().getCurrentUser();
        if (u == null) return "No user is logged in.";
        if (u.getUnlockedPlants() != null && u.getUnlockedPlants().contains(type.name()))
            return "You already own this plant.";
        if (u.getCoins() < PLANT_UNLOCK_COST_COINS)
            return "Not enough coins! Need " + PLANT_UNLOCK_COST_COINS + ", have " + u.getCoins() + ".";
        u.setCoins(u.getCoins() - PLANT_UNLOCK_COST_COINS);
        svc().getUserService().unlockPlant(u, type);
        svc().getUserRepository().save(u);
        newsSafe(n -> n.onPlantBought(u, type.name()));
        return null;
    }

    /** ارتقای گیاه با اقتصادِ فاز ۱ (سکه + کیسه‌بذر) — {@code null} موفق، وگرنه پیامِ خطا. */
    public String tryUpgradePlantCollection(PlantType type) {
        User u = app().getCurrentUser();
        if (u == null) return "No user is logged in.";
        int level = u.getPlantUpgradeLevel(type);
        if (level >= COLL_MAX_UPGRADE_LEVEL) return "Already at max level.";
        long coinCost = COLL_UPGRADE_COIN_COST[level];
        int  seedCost = COLL_UPGRADE_SEED_COST[level];
        if (u.getCoins() < coinCost)
            return "Not enough coins! Need " + coinCost + ", have " + u.getCoins() + ".";
        if (u.getSeedPackets(type) < seedCost)
            return "Not enough seed packets! Need " + seedCost + ", have " + u.getSeedPackets(type) + ".";
        u.setCoins(u.getCoins() - coinCost);
        u.spendSeedPackets(type.name(), seedCost);
        int newLvl = u.incrementPlantUpgradeLevel(type.name());
        svc().getUserRepository().save(u);
        newsSafe(n -> n.onPlantUpgraded(u, type.name(), newLvl));
        return null;
    }

    //======================================================================
    //  Quests (منوی مأموریت‌ها — فاز ۲)
    // =========================================================================

    /**
     * کوئست‌های یک دسته (page می‌تونه "daily"، "epic"، "story" یا "repeatable"
     * باشه — دقیقاً همون کلیدهایی که {@code QuestService.filterByPage} پشتیبانی
     * می‌کنه) به ترتیب اولویت (CRITICAL بالاتر از HIGH، HIGH بالاتر از...).
     */
    public List<QuestDefinition> getQuestsByCategory(String page) {
        List<QuestDefinition> list = new ArrayList<>(svc().getQuestService().filterByPage(page));
        list.sort(Comparator.comparing(QuestDefinition::getPriority).reversed());
        return list;
    }

    /** پیشرفت فعلی کاربر روی یک کوئست؛ اگر هنوز چیزی ثبت نشده یک پیشرفت خالی برمی‌گردونه. */
    public QuestProgress getQuestProgress(String questId) {
        QuestProgress qp = svc().getQuestService().getActiveProgress().get(questId);
        return qp != null ? qp : new QuestProgress(questId);
    }

    /**
     * دریافت جایزه‌ی یک کوئست تمام‌شده (دکمه‌ی CLAIM). جایزه‌ی واقعی (سکه/الماس)
     * از قبل، همان لحظه‌ی تکمیل کوئست، به‌صورت خودکار به کاربر اضافه شده؛ این
     * متد فقط وضعیت claimed رو ثبت می‌کنه تا ردیف از حالت «آماده‌ی دریافت» خارج
     * بشه. false برمی‌گردونه اگر کوئست هنوز تمام نشده یا قبلاً claim شده.
     */
    public boolean claimQuestReward(String questId) {
        return svc().getQuestService().claimQuest(questId);
    }

    /** رویدادهای تکمیلِ کوئستِ حین بازی (برای توست) — پس از خواندن پاک می‌شوند. */
    public List<String> drainQuestEvents() {
        return svc().getQuestService().drainCompletedEvents();
    }

    public List<String> getSeenZombies() {
        User u = app().getCurrentUser();
        if (u == null || u.getSeenZombies() == null) return Collections.emptyList();
        return u.getSeenZombies();
    }

    /** تمام انواع زامبی به همراه آمار ثابت و وضعیت دیده‌شدن — برای تب Zombies در صفحه کلکسیون. */
    public List<ZombieEntry> getAllZombies() {
        List<ZombieEntry> list = new ArrayList<>();
        List<String> seen = getSeenZombies();
        for (ZombieType type : ZombieType.values()) {
            com.pvz2.model.zombies.ZombieStats stats =
                    com.pvz2.model.zombies.ZombieDataRegistry.getInstance().getStats(type);
            boolean wasSeen = seen.contains(type.name());
            list.add(new ZombieEntry(type, stats, wasSeen));
        }
        return list;
    }

    public List<LeaderboardEntry> getLeaderboard(String sortKey, boolean ascending) {
        if (online()) {
            try {
                Packet resp = net().requestSync(MessageType.LEADERBOARD_REQ,
                        new com.pvz2.shared.protocol.payload.LeaderboardRequest(sortKey, ascending));
                if (MessageType.LEADERBOARD_RES.equals(resp.type)) {
                    com.pvz2.shared.protocol.payload.LeaderboardResponse lr =
                            resp.payload(Wire.GSON, com.pvz2.shared.protocol.payload.LeaderboardResponse.class);
                    List<LeaderboardEntry> out = new ArrayList<>();
                    if (lr != null && lr.entries != null) {
                        for (com.pvz2.shared.protocol.payload.LeaderboardEntryDTO e : lr.entries) {
                            // My Point column: -1 when the player never played networked scored.
                            int mp = e.hasMyPoint ? (int) e.myPoint : -1;
                            out.add(new LeaderboardEntry(e.username, e.nickname,
                                    e.lastLevel != null ? e.lastLevel : "-",
                                    e.minigameCount, e.dailyQuestCount, e.questCount, mp));
                        }
                    }
                    return out; // already sorted by the server
                }
            } catch (NetException e) {
                NetLog.info("leaderboard: server unreachable, using local: " + e.getMessage());
            }
        }
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

    /** انتشارِ بی‌خطرِ یک خبر (اگر NewsService در دسترس بود). */
    private void newsSafe(java.util.function.Consumer<NewsService> action) {
        try {
            NewsService ns = news();
            if (ns != null) action.accept(ns);
        } catch (Exception ignored) { }
    }

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
        /** سطح ارتقای گیاه (۰..۳) — از {@code User#getPlantUpgradeLevel}. */
        public int level;
        /** فعلاً معادل «سطح ارتقای کامل» — به یادداشت بالای {@link #getAllPlants()} نگاه کنید. */
        public boolean boosted;

        PlantEntry(PlantType t, PlantStats s, boolean u) { this(t, s, u, 0, false); }
        PlantEntry(PlantType t, PlantStats s, boolean u, int lvl, boolean boost) {
            type = t; stats = s; unlocked = u; level = lvl; boosted = boost;
        }
    }

    /** ورودی زامبی برای صفحه کلکسیون — آمار ثابت + وضعیت دیده‌شدن توسط کاربر. */
    public static class ZombieEntry {
        public ZombieType type;
        public com.pvz2.model.zombies.ZombieStats stats;
        public boolean seen;
        ZombieEntry(ZombieType t, com.pvz2.model.zombies.ZombieStats s, boolean seen) {
            type = t; stats = s; this.seen = seen;
        }
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
