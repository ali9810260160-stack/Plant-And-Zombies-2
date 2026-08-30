package com.pvz2.model.zombies;

import com.pvz2.model.GameSession;
import com.pvz2.model.enums.PlantEffect;
import com.pvz2.model.enums.TileType;
import com.pvz2.model.enums.ZombieType;
import com.pvz2.model.plants.Plant;
import com.pvz2.model.tiles.Tile;
import com.pvz2.util.RandomUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * زامبی پایه - ساده‌ترین نوع زامبی با منطق حرکت و حمله.
 */
public class NormalZombie extends Zombie {

    public static final double MAX_ALLSTAR_SPEED = 0.5;
    /** سرعتِ خیلی‌کمِ فوتبالیست پس از خوردنِ اولین گیاه. */
    private static final double ALLSTAR_SLOW_SPEED = 0.06;

    private int specialTimer;

    /** مجموعِ خورشیدِ دزدیده‌شده (Turquoise/Ra) — هنگامِ مرگ استفاده می‌شود. */
    private int stolenSun = 0;
    /** فوتبالیست اولین گیاه را خورد و کند شد؟ */
    private boolean allStarCharged = false;
    /** تورکوایز لیزرش را شلیک کرد؟ */
    private boolean laserFired = false;
    /** دبه‌ای/آرکید: دبه/دستگاه را (روزیْ) داشته و حالا شکسته؟ */
    private boolean sawBarrel = false;
    private boolean impsSpawned = false;

    /** مجموعِ خورشیدِ نگه‌داشته‌شده توسطِ این زامبی. */
    public int getStolenSun() { return stolenSun; }

    public NormalZombie(ZombieType type, int hp, int dps,
                        double speed, int waveCost) {
        super(type, hp, dps, speed, waveCost);
        this.specialTimer = 0;
    }

    @Override
    public void onTick(int tickCount, GameSession session) {
        tickEffects();
        handleSpecialBehavior(tickCount, session);
    }

    private void handleSpecialBehavior(int tick, GameSession session) {
        specialTimer++;
        switch (type) {
            case NEWSPAPER_ZOMBIE:
                handleNewspaper();
                break;
            case ALL_STAR:
                handleAllStar(session);
                break;
            case TURQUOISE_ZOMBIE:
                handleTurquoise(tick, session);
                break;
            case PROSPECTOR_ZOMBIE:
                handleProspector(tick, session);
                break;
            case KING_ZOMBIE:
                handleKing(tick, session);
                break;
            case RA_ZOMBIE:
                handleRaZombie(tick, session);
                break;
            case PIANIST_ZOMBIE:
                handlePianist(tick, session);
                break;
            case TOMB_RAISER:
                handleTombRaiser(session);
                break;
            case EXPLORER_ZOMBIE:
                handleExplorer(session);
                break;
            case HUNTER_ZOMBIE:
                handleHunter(session);
                break;
            case TROGLOBITE:
                handleTroglobite(session);
                break;
            case FISHERMAN_ZOMBIE:
                handleFisherman(session);
                break;
            case WIZARD_ZOMBIE:
                handleWizard(session);
                break;
            case BARREL_ROLLER:
                handleBarrelRoller(session);
                break;
            case ARCADE_ZOMBIE:
                handleArcade(session);
                break;
            default:
                break;
        }
    }

    /** جادوگر: هر ~۵ ثانیه یک گیاهِ تصادفی را به گوسفند تبدیل می‌کند (تا مرگِ جادوگر). */
    private void handleWizard(GameSession session) {
        if (specialTimer % 50 == 0) {
            List<Plant> candidates = new ArrayList<>();
            int rows = session.getGameMap().getRows();
            int cols = session.getGameMap().getCols();
            for (int r = 1; r <= rows; r++) {
                for (int c = 1; c <= cols; c++) {
                    Tile t = session.getGameMap().getTile(c, r);
                    if (t != null && t.getPlant() != null && !t.getPlant().isSheep()) {
                        candidates.add(t.getPlant());
                    }
                }
            }
            if (candidates.isEmpty()) return;
            Plant p = candidates.get(RandomUtil.between(0, candidates.size() - 1));
            p.addEffect(PlantEffect.WIZARDED, 99999);   // تا مرگِ جادوگر پابرجا می‌ماند
        }
    }

    /**
     * ماهیگیر: در راست‌ترین ستون ثابت می‌ماند و هر ~۴ ثانیه نزدیک‌ترین گیاهِ ردیفش را
     * قلاب کرده یک خانه به سمتِ خودش (راست) می‌کشد؛ اگر چسبیده به او باشد نابودش می‌کند.
     */
    private boolean fishermanAnchored = false;
    private void handleFisherman(GameSession session) {
        int cols = session.getGameMap().getCols();
        if (!fishermanAnchored) {
            if (x <= cols) {          // به لبه‌ی راستِ زمین رسید → لنگر بینداز و ثابت شو
                x = cols;
                moveSpeed = 0;
                fishermanAnchored = true;
            }
            return;
        }
        if (specialTimer % 40 == 0) {
            hookPlant(session, cols);
        }
    }

    private void hookPlant(GameSession session, int cols) {
        for (int c = cols; c >= 1; c--) {          // نزدیک‌ترین گیاه به ماهیگیر
            if (!session.getGameMap().isValidPosition(c, y)) continue;
            Tile t = session.getGameMap().getTile(c, y);
            if (t == null || t.getPlant() == null) continue;
            com.pvz2.model.plants.Plant p = t.getPlant();
            Tile right = session.getGameMap().isValidPosition(c + 1, y)
                    ? session.getGameMap().getTile(c + 1, y) : null;
            if (c >= cols || right == null || right.getPlant() != null || !right.isPlantable()) {
                p.setCurrentHealth(0);             // چسبیده/جای خالی نبود → نابود
            } else {
                right.setPlant(p);                 // یک خانه به راست بکش
                t.setPlant(null);
                p.setX(c + 1);
            }
            return;
        }
    }

    /** شکارچی: هر ~۲ ثانیه به نزدیک‌ترین گیاهِ ردیفش یخ پرتاب می‌کند (۳ ضربه = یخِ کامل). */
    private void handleHunter(GameSession session) {
        if (specialTimer % 20 == 0) {
            int cols = session.getGameMap().getCols();
            int start = Math.min(cols, (int) Math.round(x));
            for (int c = start; c >= 1; c--) {
                if (!session.getGameMap().isValidPosition(c, y)) continue;
                Tile t = session.getGameMap().getTile(c, y);
                if (t != null && t.getPlant() != null && !t.getPlant().isFirePlant()) {
                    t.getPlant().incrementFreezeLevel();
                    return;
                }
            }
        }
    }

    /** تروگلوبایت: بلوکِ یخ را جلو می‌راند؛ به اولین گیاه که برسد، گیاه و بلوک نابود می‌شوند. */
    private boolean iceBlockIntact = true;
    private void handleTroglobite(GameSession session) {
        if (!iceBlockIntact) return;
        int frontCol = (int) Math.round(x) - 1;
        if (frontCol < 1) return;
        if (!session.getGameMap().isValidPosition(frontCol, y)) return;
        Tile t = session.getGameMap().getTile(frontCol, y);
        if (t != null && t.getPlant() != null) {
            t.getPlant().setCurrentHealth(0);   // بلوکِ یخ گیاهِ جلویی را له می‌کند
            iceBlockIntact = false;             // بلوک مصرف شد
        }
    }

    /** قبرساز: هر ~۱۲ ثانیه دو استخوان پرتاب می‌کند و در دو خانه‌ی تصادفی قبر می‌سازد. */
    private void handleTombRaiser(GameSession session) {
        if (specialTimer % 120 == 0) {
            raiseTombstones(session, 2);
        }
    }

    private void raiseTombstones(GameSession session, int count) {
        int rows = session.getGameMap().getRows();
        int cols = session.getGameMap().getCols();
        int placed = 0, attempts = 0;
        while (placed < count && attempts < 30) {
            attempts++;
            int c = RandomUtil.between(1, cols);
            int r = RandomUtil.between(1, rows);
            Tile t = session.getGameMap().getTile(c, r);
            if (t != null && t.getPlant() == null && t.isPlantable() && !t.isTombstone()) {
                session.getGameMap().setTileType(c, r, TileType.TOMBSTONE);
                placed++;
            }
        }
    }

    /** مشعل‌دار: تا وقتی مشعلش روشن است، گیاهِ یک‌خانه جلوترش را می‌سوزاند. */
    private void handleExplorer(GameSession session) {
        if (!isTorchLit()) return;
        int frontCol = (int) Math.round(x) - 1;   // یک خانه جلوتر (به سمتِ خانه)
        if (frontCol < 1) return;
        if (!session.getGameMap().isValidPosition(frontCol, y)) return;
        Tile t = session.getGameMap().getTile(frontCol, y);
        if (t != null && t.getPlant() != null && !t.getPlant().isFrozen()) {
            t.getPlant().setCurrentHealth(0);   // مشعل گیاهِ جلویی را نابود می‌کند
        }
    }

    private void handleNewspaper() {
        if (!hasArmor(com.pvz2.model.enums.ArmorType.NEWSPAPER)
                && moveSpeed < 0.4) {
            moveSpeed = 0.4;
        }
    }

    /**
     * فوتبالیست: با سرعتِ زیاد می‌دود و در حینِ دویدن هر گیاه یا زامبیِ هیپنوتیزم‌شده‌ای
     * را که به آن برسد با یک ضربه‌ی مهلک درجا نابود می‌کند؛ پس از خوردنِ «اولین گیاه»
     * سرعتش به‌شدت کم می‌شود و مثلِ زامبیِ عادی رفتار می‌کند.
     */
    private void handleAllStar(GameSession session) {
        if (allStarCharged) return;                 // دیگر شارژ نمی‌کند
        if (moveSpeed < MAX_ALLSTAR_SPEED) moveSpeed = MAX_ALLSTAR_SPEED;

        // زامبی‌های هیپنوتیزم‌شده‌ی سرِ راه را له می‌کند (بدونِ توقفِ شارژ).
        for (Zombie z : session.getActiveZombies()) {
            if (z != this && z.isAlive() && z.isHypnotized()
                    && z.getY() == y && Math.abs(z.getX() - x) < 0.6) {
                z.setCurrentHealth(0);
            }
        }

        int col = (int) Math.ceil(x);               // خانه‌ی پیشِ رو (هم‌راستا با isBlockedByPlant)
        if (!session.getGameMap().isValidPosition(col, y)) return;
        Tile t = session.getGameMap().getTile(col, y);
        if (t != null && t.getPlant() != null && !t.getPlant().isSheep()) {
            t.getPlant().setCurrentHealth(0);       // ضربه‌ی مهلک به اولین گیاه
            allStarCharged = true;
            moveSpeed = ALLSTAR_SLOW_SPEED;          // خیلی کند می‌شود
            setAttacking(false);
        }
    }

    /**
     * تورکوایز: در ۵ ثانیه‌ی نخست هر ثانیه ۲۵ خورشید از بازیکن می‌دزدد (مجموعاً تا ۱۲۵)،
     * سپس یک‌بار یک «لیزر» به ۴ خانه‌ی روبه‌رویش در همان ردیف شلیک کرده همه‌ی آن گیاهان را
     * نابود می‌کند. نیمی از خورشیدِ دزدیده‌شده هنگامِ مرگ روی زمین می‌افتد
     * ({@link com.pvz2.service.CombatService#handleZombieDeath}).
     */
    private void handleTurquoise(int tick, GameSession session) {
        if (specialTimer <= 50) {                     // مرحله‌ی دزدی (۵ ثانیه)
            // انیمیشنِ power_up→power توسطِ abilityِ ON_ENTER (در character_animations.json)
            // خودکار پخش می‌شود؛ اینجا فقط منطقِ دزدیدنِ خورشید است.
            if (specialTimer % 10 == 0) {
                int stolen = Math.min(25, session.getSunAmount());
                session.setSunAmount(session.getSunAmount() - stolen);
                stolenSun += stolen;
            }
        } else if (specialTimer == 51) {               // پایانِ دزدی → کلیپِ power_down
            fireAnimEvent(com.pvz2.model.enums.AnimEvent.SPECIAL_ABILITY_ENDED);
        } else if (specialTimer >= 60 && !laserFired) { // پس از مکثِ کوتاه: شلیکِ لیزر (کلیپِ attack)
            laserFired = true;
            fireLaser(session);
        }
    }

    /** لیزرِ تورکوایز: ۴ خانه‌ی روبه‌رو (به سمتِ خانه) در همان ردیف را نابود می‌کند. */
    private void fireLaser(GameSession session) {
        int startCol = (int) x - 1;
        for (int i = 0; i < 4; i++) {
            int col = startCol - i;
            if (col < 1) break;
            if (!session.getGameMap().isValidPosition(col, y)) continue;
            Tile t = session.getGameMap().getTile(col, y);
            if (t != null && t.getPlant() != null) {
                t.getPlant().setCurrentHealth(0);
            }
        }
        // پرتوی بصری برای لایه‌ی گرافیک (۴ خانه در همان ردیف).
        session.addLaserZap(y, startCol, Math.max(1, startCol - 3));
        fireAnimEvent(com.pvz2.model.enums.AnimEvent.SPECIAL_ABILITY_FIRED);
    }

    private boolean dynamiteBlown = false;

    /**
     * اکتشافگر: دینامیتِ پشتش بعد از ~۱۰ ثانیه منفجر می‌شود و جهتش برعکسِ بقیه‌ی
     * زامبی‌ها می‌شود (به سمتِ راست). دیگر از زمین خارج نمی‌شود: هر بار به لبه برسد
     * جهتش عوض می‌شود. تا وقتی از راستِ زمین وارد شبکه نشده، فقط به سمتِ چپ می‌آید.
     */
    private void handleProspector(int tick, GameSession session) {
        int cols = session.getGameMap().getCols();
        boolean inGrid = x <= cols && x >= 1;

        // انفجارِ دینامیت (۱۰ ثانیه): زامبی به اولین کاشیِ نزدیکِ خانه (ستونِ ۱)
        // پرتاب می‌شود و سپس برعکسِ بقیه‌ی زامبی‌ها (به سمتِ راست) حرکت کرده و
        // گیاهانِ سرِ راهش را می‌خورد.
        if (specialTimer >= 100 && !dynamiteBlown && inGrid) {
            dynamiteBlown = true;
            x = 1;                   // پرتاب به اولین کاشیِ نزدیکِ خانه
            movingBackward = true;   // سپس به سمتِ راست (خلافِ جهت)
        }

        // بازگشت در لبه‌ها به‌جای خروج از زمین (نوسان داخلِ شبکه).
        if (dynamiteBlown && inGrid) {
            if (movingBackward && x >= cols) movingBackward = false;       // لبه‌ی راست → چپ
            else if (!movingBackward && x <= 1) movingBackward = true;     // لبه‌ی چپ → راست
        }
    }

    /** پادشاه: در راست‌ترین ستون ثابت می‌ماند و هر ~۵ ثانیه زامبی‌های سـاده‌ی اطرافش
     *  را به شوالیه (کلاه‌خود + شانه‌بند) ارتقا می‌دهد. */
    private boolean kingAnchored = false;
    private void handleKing(int tick, GameSession session) {
        int cols = session.getGameMap().getCols();
        if (!kingAnchored) {
            if (x <= cols) { x = cols; moveSpeed = 0; kingAnchored = true; }
            return;
        }
        if (specialTimer % 50 == 0) {
            upgradeNearbyZombies(session);
        }
    }

    private void upgradeNearbyZombies(GameSession session) {
        for (Zombie z : session.getActiveZombies()) {
            if (z.getType() == ZombieType.NORMAL
                    && Math.abs(z.getX() - x) <= 2
                    && Math.abs(z.getY() - y) <= 1) {
                z.addArmor(com.pvz2.model.enums.ArmorType.HELMET, 1600);
                z.addArmor(com.pvz2.model.enums.ArmorType.SHOULDER_ARMOR, 1600);
            }
        }
    }

    private boolean raStealing = false;
    private int     raStealTimer = 0;
    private double  raSavedSpeed = 0;
    private static final int RA_STEAL_TICKS = 40;   // ۴ ثانیه

    /**
     * Ra: هر ~۶ ثانیه چوبش را بالا می‌آورد؛ خورشیدهای روی زمینِ همان ردیف «بنفش»
     * شده و طیِ ۴ ثانیه به‌سمتِ چوبش کشیده و دزدیده می‌شوند. در این مدت خود Ra ثابت
     * می‌ماند (چوب‌بالا) و پس از پایان به حرکتش ادامه می‌دهد. خورشیدهای دزدیده‌شده
     * هنگامِ مرگ به بازیکن برمی‌گردند
     * ({@link com.pvz2.service.CombatService#handleZombieDeath}).
     */
    private void handleRaZombie(int tick, GameSession session) {
        if (session.getActiveSuns() == null) return;

        if (raStealing) {
            raStealTimer++;
            // هر ~۱ ثانیه ژستِ «چوب‌بالا» را دوباره تریگر کن تا در کلِ ۴ ثانیه‌ی
            // کشیدنِ خورشیدها Ra در همان حالت باقی بماند.
            if (raStealTimer % 10 == 0 && raStealTimer < RA_STEAL_TICKS) {
                fireAnimEvent(com.pvz2.model.enums.AnimEvent.SPECIAL_ABILITY_FIRED);
            }
            double p = Math.min(1.0, raStealTimer / (double) RA_STEAL_TICKS);
            for (com.pvz2.model.Sun s : session.getActiveSuns()) {
                if (s.isBeingStolen()) s.setStealProgress(p);
            }
            if (raStealTimer >= RA_STEAL_TICKS) {   // پایانِ کشش → دزدی
                session.getActiveSuns().removeIf(s -> {
                    if (s.isBeingStolen() && !s.isCollected()) {
                        stolenSun += s.getValue();
                        return true;
                    }
                    return false;
                });
                raStealing = false;
                moveSpeed = raSavedSpeed;            // ادامه‌ی حرکت
            }
            return;
        }

        // شروعِ دوره‌ای (هر ۶ ثانیه): اگر خورشیدی روی زمینِ همین ردیف باشد، دزدی را آغاز کن.
        if (specialTimer % 60 == 0) {
            boolean any = false;
            for (com.pvz2.model.Sun s : session.getActiveSuns()) {
                if (s.isLanded() && !s.isCollected() && !s.isBeingStolen()
                        && s.getY() == y) {
                    s.setBeingStolen(true);
                    s.setStealer((int) x, y);
                    s.setStealProgress(0);
                    any = true;
                }
            }
            if (any) {
                raStealing   = true;
                raStealTimer = 0;
                raSavedSpeed = moveSpeed;
                moveSpeed    = 0;                    // چوب‌بالا و ثابت‌ماندن
                fireAnimEvent(com.pvz2.model.enums.AnimEvent.SPECIAL_ABILITY_FIRED);
            }
        }
    }

    private void handlePianist(int tick, GameSession session) {
        if (specialTimer % 30 == 0) {
            shiftNearbyZombies(session);
        }
    }

    private void shiftNearbyZombies(GameSession session) {
        int rows = session.getGameMap().getRows();
        for (Zombie z : session.getActiveZombies()) {
            if (z == this) {
                continue;
            }
            if (Math.abs(z.getX() - x) <= 3) {
                int dir = Math.random() > 0.5 ? 1 : -1;
                int newY = z.getY() + dir;
                if (newY >= 1 && newY <= rows) {
                    z.setY(newY);
                    z.setLane(newY);
                }
            }
        }
    }

    /**
     * زامبیِ دبه‌ای: دبه (زره BARREL) جلوی او را می‌گیرد و ضربه‌ها را جذب می‌کند؛
     * وقتی دبه شکست، ۲ ایمپ در همان ردیف بیرون می‌آیند.
     */
    private void handleBarrelRoller(GameSession session) {
        if (hasArmor(com.pvz2.model.enums.ArmorType.BARREL)) {
            sawBarrel = true;
            return;
        }
        if (sawBarrel && !impsSpawned) {
            impsSpawned = true;
            spawnImps(session, 2);
        }
    }

    /**
     * زامبیِ آرکید: دستگاهِ آرکید (زره BARREL با جانِ سطلی) را جلو می‌راند؛ تا وقتی
     * دستگاه سالم است، هر گیاه یا زامبیِ هیپنوتیزم‌شده‌ی جلویش را نابود می‌کند
     * (مثلِ تروگلوبایت اما پیوسته). با شکستنِ دستگاه، ۲ ایمپ بیرون می‌آیند.
     */
    private void handleArcade(GameSession session) {
        if (hasArmor(com.pvz2.model.enums.ArmorType.BARREL)) {
            sawBarrel = true;
            crushFrontObstacles(session);
            return;
        }
        if (sawBarrel && !impsSpawned) {
            impsSpawned = true;
            spawnImps(session, 2);
        }
    }

    /** خانه‌ی یک‌قدم جلوتر را از گیاه و زامبیِ هیپنوتیزم‌شده پاک می‌کند. */
    private void crushFrontObstacles(GameSession session) {
        int frontCol = (int) Math.round(x) - 1;
        if (frontCol >= 1 && session.getGameMap().isValidPosition(frontCol, y)) {
            Tile t = session.getGameMap().getTile(frontCol, y);
            if (t != null && t.getPlant() != null) {
                t.getPlant().setCurrentHealth(0);
            }
        }
        for (Zombie z : session.getActiveZombies()) {
            if (z != this && z.isAlive() && z.isHypnotized()
                    && z.getY() == y && Math.abs(z.getX() - x) < 1.0) {
                z.setCurrentHealth(0);
            }
        }
    }

    /** ساختِ چند ایمپ در ردیفِ همین زامبی، کمی جلوترش (به سمتِ خانه). */
    private void spawnImps(GameSession session, int count) {
        if (session.getActiveZombies() == null) return;
        for (int i = 0; i < count; i++) {
            ImpZombie imp = new ImpZombie(ZombieType.IMP, 0.22, 100);
            imp.setX(Math.max(1.0, x - 0.3 * (i + 1)));
            imp.setY(y);
            imp.setLane(y);
            imp.setSpawnWave(spawnWave);
            session.getActiveZombies().add(imp);
        }
    }

    @Override
    public String getDescription() {
        return type.name() + " - a zombie with "
               + maxHealth + " HP and "
               + damagePerSecond + " DPS.";
    }
}
