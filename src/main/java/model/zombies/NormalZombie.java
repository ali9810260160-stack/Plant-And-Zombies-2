package model.zombies;

import model.GameMap;
import model.GameSession;
import model.Projectile;
import model.Sun;
import model.enums.ArmorType;
import model.enums.PlantEffect;
import model.enums.ProjectileType;
import model.enums.SunType;
import model.enums.TileType;
import model.enums.ZombieEffect;
import model.enums.ZombieType;
import model.plants.Plant;
import model.tiles.Tile;
import util.RandomUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * زامبی معمولی و تمام زیرگونه‌های آن که رفتار خاص خودشان را دارند اما
 * نیازی به یک کلاس جداگانه ندارند (سرسطلی، شوالیه، سربلوکی، فوتبالیست،
 * زامبی آرکید، چتردار، تورکوایز، اکتشافگر، پیانیست، پیرمرد روزنامه‌دار،
 * زامبی دبه‌ای، خورشیددزد، مشعل‌دار، قبرساز، دودو سوار، شکارچی،
 * تروگلوبایت، ماهیگیر، غواص، اختاپوس‌پرت‌کن، جادوگر، پادشاه و زامبی‌های
 * مینی‌گیم‌های Zombotany و I,Zombie).
 * رفتار ویژه هر نوع در متد onTick بر اساس فیلد type اجرا می‌شود.
 */
public class NormalZombie extends Zombie {

    /** AllStar Zombie */
    /** برخورد با زامبی هیپنوتیزم شده یا گیاه */
    protected boolean collidWithHypnoZombieOrPlant;
    protected static double MAX_ALLSTAR_SPEED = 0.3;

    /** آیا فوتبالیست قبلاً با گیاه/زامبی هیپنوتیزم‌شده برخورد کرده و کند شده است */
    private boolean allStarSlowed;
    /** سرعت آرام فوتبالیست بعد از برخورد اول (همان سرعت اولیه ساخته‌شده توسط factory) */
    private double allStarSlowSpeed;
    /** آیا سرعت اولیه سریع فوتبالیست تنظیم شده است */
    private boolean allStarStarted;

    /** FisherMan Zombie */
    private int cooldownForFishing = 50; // برحسب تیک

    /** Arcade Zombie / Barrel Roller: آیا دبه شکسته شده و ایمپ‌ها بیرون آمده‌اند */
    private boolean barrelImpsSpawned;

    /** Turquoise Zombie */
    private int turquoiseStolenSun;
    private int turquoiseStealTicksRemaining;
    private boolean turquoiseFired;

    /** Prospector Zombie */
    private int dynamiteFuseTicks = 100; // 10 ثانیه = 100 تیک
    private boolean dynamiteExploded;
    private boolean dynamiteExtinguished;

    /** Pianist Zombie */
    private int pianistCooldown = 20;

    /** Newspaper Zombie */
    private boolean newspaperEnraged;

    /** Ra Zombie */
    private int raStolenSunAmount;

    /** Explorer Zombie */
    private boolean torchLit = true;

    /** Tomb Raiser */
    private int tombRaiserCooldown = 30;

    /** Hunter Zombie / Octopus Zombie */
    private int hunterCooldown = 20;
    private int octopusCooldown = 20;

    /** Wizard Zombie */
    private int wizardCooldown = 30;
    private final List<Plant> wizardedPlants = new ArrayList<>();

    /** King Zombie */
    private int kingCooldown = 50;

    /** Snorkel Zombie */
    private boolean submerged = true;

    /** Zombotany: Jalapeno */
    private int ticksSinceSpawn;
    private static final int JALAPENO_FUSE_TICKS = 100;

    /** Zombotany: Peashooter */
    private int peashooterCooldown = 14;

    /** Sun Producer Zombie (مینی‌گیم I, Zombie) */
    private int sunProducerCooldown = 100;

    public NormalZombie(ZombieType type, int health, int waveCost, double moveSpeed, int dPS) {
        this.type = type;
        this.maxHealth = health;
        this.currentHealth = health;
        this.moveSpeed = moveSpeed;  // خانه بر ثانیه
        this.damagePerSecond = dPS;
        this.waveCost = waveCost;
        this.armors = new LinkedHashMap<>();
        this.activeEffects = new LinkedHashMap<>();
    }

    @Override
    public void onTick(int tickCount, GameSession gameSession) {
        switch (type) {
            case NORMAL:
            case BUCKETHEAD:
            case BLOCKHEAD:
            case KNIGHT:
            case PARASOL_ZOMBIE:
            case DODO_RIDER:
            case ZOMBOTANY_WALLNUT:
                // این زامبی‌ها رفتار ویژه‌ای در هر تیک ندارند؛ تفاوتشان در جان/زره/سرعت است
                break;
            case ALL_STAR:
                allStarTick(gameSession);
                break;
            case ARCADE_ZOMBIE:
            case BARREL_ROLLER:
                pushingContainerTick(gameSession);
                break;
            case TURQUOISE_ZOMBIE:
                turquoiseTick(gameSession);
                break;
            case PROSPECTOR_ZOMBIE:
                prospectorTick();
                break;
            case PIANIST_ZOMBIE:
                pianistTick(gameSession);
                break;
            case NEWSPAPER_ZOMBIE:
                newspaperTick();
                break;
            case RA_ZOMBIE:
                raTick(gameSession);
                break;
            case EXPLORER_ZOMBIE:
                explorerTick(gameSession);
                break;
            case TOMB_RAISER:
                tombRaiserTick(gameSession);
                break;
            case HUNTER_ZOMBIE:
                hunterTick(gameSession);
                break;
            case TROGLOBITE:
                troglobiteTick(gameSession);
                break;
            case FISHERMAN_ZOMBIE:
                fishermanTick(gameSession, tickCount);
                break;
            case SNORKEL_ZOMBIE:
                snorkelTick(gameSession);
                break;
            case OCTOPUS_ZOMBIE:
                octopusTick(gameSession);
                break;
            case WIZARD_ZOMBIE:
                wizardTick(gameSession);
                break;
            case KING_ZOMBIE:
                kingTick(gameSession);
                break;
            case ZOMBOTANY_PEASHOOTER:
                zombotanyPeashooterTick(gameSession);
                break;
            case ZOMBOTANY_JALAPENO:
                zombotanyJalapenoTick(gameSession);
                break;
            case ZOMBOTANY_SQUASH:
                zombotanySquashTick(gameSession);
                break;
            case SUN_PRODUCER_ZOMBIE:
                sunProducerTick(gameSession);
                break;
            default:
                break;
        }
    }

    @Override
    public void onDeath(GameSession gameSession) {
        switch (type) {
            case TURQUOISE_ZOMBIE:
                // پس از کشته شدن، نیمی از خورشیدهای دزدیده‌شده را می‌اندازد
                gameSession.setSunAmount(gameSession.getSunAmount() + turquoiseStolenSun / 2);
                break;
            case RA_ZOMBIE:
                // بعد از کشته شدن، تمام خورشیدهای دزدیده‌شده به بازیکن بازمی‌گردد
                gameSession.setSunAmount(gameSession.getSunAmount() + raStolenSunAmount);
                break;
            case WIZARD_ZOMBIE:
                // با مرگ جادوگر، گیاهانی که به گربه تبدیل شده‌اند به حالت عادی بازمی‌گردند
                for (Plant plant : wizardedPlants) {
                    plant.removeEffect(PlantEffect.WIZARDED);
                }
                wizardedPlants.clear();
                break;
            default:
                break;
        }
    }

    // ==================== فوتبالیست (All Star) ====================

    /**
     * فوتبالیست با سرعت زیاد وارد می‌شود. در برخورد اول با گیاه یا زامبی
     * هیپنوتیزم‌شده، یک ضربه مهلک وارد کرده و از آن پس با سرعت بسیار آرام حرکت می‌کند.
     */
    private void allStarTick(GameSession gameSession) {
        if (!allStarStarted) {
            allStarSlowSpeed = moveSpeed;
            moveSpeed = MAX_ALLSTAR_SPEED;
            allStarStarted = true;
        }
        if (allStarSlowed) {
            return;
        }
        GameMap map = gameSession.getGameMap();
        Tile tile = map.getTile((int) x, y);
        if (tile == null) {
            return;
        }
        boolean hitPlant = tile.getPlant() != null;
        boolean hitHypnoZombie = isHypnoZombieOnTile(tile);
        if (hitPlant) {
            tile.getPlant().setCurrentHealth(0);
            tile.setPlant(null);
            allStarSlowed = true;
            moveSpeed = allStarSlowSpeed;
        } else if (hitHypnoZombie) {
            allStarSlowed = true;
            moveSpeed = allStarSlowSpeed;
        }
    }

    // ==================== دستگاه آرکید / زامبی دبه‌ای ====================

    /**
     * زامبی آرکید و زامبی دبه‌ای، وسیله‌ای (دستگاه/دبه) را جلوی خود هل می‌دهند
     * که به شکل زره BARREL مدل‌سازی شده است. برخورد این وسیله با گیاه یا زامبی
     * هیپنوتیزم‌شده باعث نابودی فوری آن می‌شود. با نابود شدن دبه، در زامبی دبه‌ای
     * دو ایمپ از همان خانه بیرون می‌آیند.
     */
    private void pushingContainerTick(GameSession gameSession) {
        if (!hasArmor(ArmorType.BARREL)) {
            if (type == ZombieType.BARREL_ROLLER && !barrelImpsSpawned) {
                spawnBarrelImps(gameSession);
            }
            return;
        }
        GameMap map = gameSession.getGameMap();
        Tile tile = map.getTile((int) x, y);
        if (tile == null) {
            return;
        }
        if (tile.getPlant() != null || isHypnoZombieOnTile(tile)) {
            removeArmor(ArmorType.BARREL);
        }
    }

    private void spawnBarrelImps(GameSession gameSession) {
        barrelImpsSpawned = true;
        List<Zombie> activeZombies = gameSession.getActiveZombies();
        GameMap map = gameSession.getGameMap();
        Tile tile = map.getTile((int) x, y);
        for (int i = 0; i < 2; i++) {
            ImpZombie imp = new ImpZombie(ZombieType.IMP, 0.22, 100);
            imp.setX(x);
            imp.setY(y);
            if (activeZombies != null) {
                activeZombies.add(imp);
            }
            if (tile != null && tile.getZombiesOnTile() != null) {
                tile.getZombiesOnTile().add(imp);
            }
        }
    }

    // ==================== تورکوایز (Turquoise Zombie) ====================

    /**
     * اگر گیاهی در شعاع 4 خانه‌ای ببیند، برای 5 ثانیه هر ثانیه 25 خورشید می‌دزدد،
     * سپس لیزری به 4 خانه جلوی خودش شلیک می‌کند که تمام گیاهان آن خانه‌ها را نابود می‌کند.
     */
    private void turquoiseTick(GameSession gameSession) {
        GameMap map = gameSession.getGameMap();
        if (turquoiseStealTicksRemaining <= 0 && !turquoiseFired) {
            if (seesPlantInRadius(map, 4)) {
                turquoiseStealTicksRemaining = 50; // 5 ثانیه
            }
        }
        if (turquoiseStealTicksRemaining > 0) {
            turquoiseStealTicksRemaining--;
            if (turquoiseStealTicksRemaining % 10 == 0) {
                int stolenNow = Math.min(25, gameSession.getSunAmount());
                gameSession.setSunAmount(gameSession.getSunAmount() - stolenNow);
                turquoiseStolenSun += stolenNow;
            }
            if (turquoiseStealTicksRemaining == 0) {
                fireLaser(map);
                turquoiseFired = true;
            }
        }
    }

    private boolean seesPlantInRadius(GameMap map, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int tx = (int) x + dx;
                int ty = y + dy;
                if (map.isValidPosition(tx, ty)) {
                    Tile tile = map.getTile(tx, ty);
                    if (tile != null && tile.getPlant() != null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void fireLaser(GameMap map) {
        for (int i = 1; i <= 4; i++) {
            int tx = (int) x - i;
            if (map.isValidPosition(tx, y)) {
                Tile tile = map.getTile(tx, y);
                if (tile != null && tile.getPlant() != null) {
                    tile.getPlant().setCurrentHealth(0);
                    tile.setPlant(null);
                }
            }
        }
    }

    // ==================== اکتشافگر (Prospector Zombie) ====================

    /**
     * دینامیت پشت اکتشافگر بعد از 10 ثانیه منفجر می‌شود؛ پس از انفجار، دینامیت
     * به انتهای سطر پرتاب شده و برخلاف جهت باقی زامبی‌ها حرکت می‌کند.
     * تیر یخی می‌تواند دینامیت را خاموش کند (extinguishDynamite).
     */
    private void prospectorTick() {
        if (dynamiteExploded || dynamiteExtinguished) {
            return;
        }
        dynamiteFuseTicks--;
        if (dynamiteFuseTicks <= 0) {
            dynamiteExploded = true;
            movingBackward = true;
        }
    }

    /** با برخورد تیر یخی، دینامیت این زامبی خاموش می‌شود */
    public void extinguishDynamite() {
        dynamiteExtinguished = true;
    }

    // ==================== پیانیست (Pianist Zombie) ====================

    /**
     * پیانیست هر چند ثانیه یک‌بار، سطر خود را با یکی از سطرهای همسایه جابه‌جا می‌کند.
     */
    private void pianistTick(GameSession gameSession) {
        pianistCooldown--;
        if (pianistCooldown > 0) {
            return;
        }
        pianistCooldown = RandomUtil.nextInt(20, 40);
        GameMap map = gameSession.getGameMap();
        int rows = map.getRows();
        int newLane;
        if (rows <= 1) {
            return;
        }
        if (y <= 1) {
            newLane = y + 1;
        } else if (y >= rows) {
            newLane = y - 1;
        } else if (RandomUtil.chance(0.5)) {
            newLane = y - 1;
        } else {
            newLane = y + 1;
        }
        this.y = newLane;
    }

    // ==================== پیرمرد روزنامه‌دار (Newspaper Zombie) ====================

    /**
     * بعد از نابود شدن روزنامه (زره NEWSPAPER)، سرعت و آسیب این زامبی چند برابر می‌شود.
     */
    private void newspaperTick() {
        if (!newspaperEnraged && !hasArmor(ArmorType.NEWSPAPER)) {
            newspaperEnraged = true;
            moveSpeed = moveSpeed * 3;
            damagePerSecond = damagePerSecond * 3;
        }
    }

    // ==================== خورشیددزد (Ra Zombie) ====================

    /**
     * خورشیدهای روی زمین را به سمت خودش می‌کشد و می‌دزدد.
     * بعد از کشته شدنش، تمام خورشیدهای دزدیده‌شده به بازیکن بازمی‌گردد (در onDeath).
     */
    private void raTick(GameSession gameSession) {
        List<Sun> suns = gameSession.getActiveSuns();
        if (suns == null) {
            return;
        }
        Iterator<Sun> iterator = suns.iterator();
        while (iterator.hasNext()) {
            Sun sun = iterator.next();
            if (sun.isLanded() && sun.getTargetY() == y) {
                raStolenSunAmount += sun.getValue();
                iterator.remove();
            }
        }
    }

    // ==================== مشعل‌دار (Explorer Zombie) ====================

    /**
     * با مشعل روشن، گیاهان هم‌سطر با فاصله حداکثر یک خانه جلوتر را نابود می‌کند.
     * تیر/پرتابه یخی مشعل را خاموش و آتشین دوباره روشن می‌کند.
     */
    private void explorerTick(GameSession gameSession) {
        if (!torchLit) {
            return;
        }
        GameMap map = gameSession.getGameMap();
        for (int dx = 0; dx <= 1; dx++) {
            int tx = (int) x - dx;
            if (map.isValidPosition(tx, y)) {
                Tile tile = map.getTile(tx, y);
                if (tile != null && tile.getPlant() != null) {
                    tile.getPlant().setCurrentHealth(0);
                    tile.setPlant(null);
                }
            }
        }
    }

    /** مشعل توسط تیر/پرتابه یخی خاموش می‌شود */
    public void extinguishTorch() {
        torchLit = false;
    }

    /** مشعل توسط تیر/پرتابه آتشین دوباره روشن می‌شود */
    public void relightTorch() {
        torchLit = true;
    }

    public boolean isTorchLit() {
        return torchLit;
    }

    // ==================== قبرساز (Tomb Raiser) ====================

    /**
     * هر چند ثانیه یک‌بار دو استخوان به دو خانه تصادفی خالی از نقشه پرتاب می‌کند
     * که در آنجا قبر تشکیل می‌شود.
     */
    private void tombRaiserTick(GameSession gameSession) {
        tombRaiserCooldown--;
        if (tombRaiserCooldown > 0) {
            return;
        }
        tombRaiserCooldown = 30;
        GameMap map = gameSession.getGameMap();
        for (int i = 0; i < 2; i++) {
            int randomX = RandomUtil.nextInt(1, map.getCols());
            int randomY = RandomUtil.nextInt(1, map.getRows());
            Tile tile = map.getTile(randomX, randomY);
            if (tile != null && tile.getPlant() == null && !tile.isTombstone()) {
                tile.setType(TileType.TOMBSTONE);
            }
        }
    }

    // ==================== شکارچی (Hunter Zombie) ====================

    /**
     * هر چند ثانیه یک‌بار به نزدیک‌ترین گیاه در سطر خود یخ پرتاب می‌کند.
     * سه بار برخورد یخ، گیاه را کاملاً یخ‌زده می‌کند.
     */
    private void hunterTick(GameSession gameSession) {
        hunterCooldown--;
        if (hunterCooldown > 0) {
            return;
        }
        hunterCooldown = 20;
        Plant target = getClosestPlantInLane(gameSession.getGameMap());
        if (target == null || target.hasEffect(PlantEffect.FROZEN)) {
            return;
        }
        if (target.hasEffect(PlantEffect.ICE_WIND_LVL2)) {
            target.removeEffect(PlantEffect.ICE_WIND_LVL2);
            target.addEffect(PlantEffect.FROZEN, Integer.MAX_VALUE);
        } else if (target.hasEffect(PlantEffect.ICE_WIND_LVL1)) {
            target.removeEffect(PlantEffect.ICE_WIND_LVL1);
            target.addEffect(PlantEffect.ICE_WIND_LVL2, Integer.MAX_VALUE);
        } else {
            target.addEffect(PlantEffect.ICE_WIND_LVL1, Integer.MAX_VALUE);
        }
    }

    private Plant getClosestPlantInLane(GameMap map) {
        for (int tx = (int) x; tx >= 1; tx--) {
            if (!map.isValidPosition(tx, y)) {
                continue;
            }
            Tile tile = map.getTile(tx, y);
            if (tile != null && tile.getPlant() != null) {
                return tile.getPlant();
            }
        }
        return null;
    }

    // ==================== تروگلوبایت (Troglobite) ====================

    /**
     * یخ‌های روی زمین را جلو هل می‌دهد. برخورد یخ با گیاه یا زامبی هیپنوتیزم‌شده
     * باعث نابودی فوری آن می‌شود.
     */
    private void troglobiteTick(GameSession gameSession) {
        GameMap map = gameSession.getGameMap();
        Tile tile = map.getTile((int) x, y);
        if (tile == null) {
            return;
        }
        if (tile.getPlant() != null || isHypnoZombieOnTile(tile)) {
            tile.setPlant(null);
        }
    }

    // ==================== ماهیگیر (Fisherman Zombie) ====================

    /**
     * در راست‌ترین ستون ثابت می‌ماند و هر چند ثانیه یک‌بار یک گیاه از سطر خودش
     * را با قلاب یک خانه به جلو می‌آورد. اگر گیاه در کنارش باشد آن را نابود می‌کند.
     */
    public void fishermanTick(GameSession gameSession, int tickCount) {
        GameMap map = gameSession.getGameMap();

        if ((int) x >= map.getCols()) {
            moveSpeed = 0;
        }

        Tile tile = map.getTile((int) x, y);
        if (tile != null && tile.getPlant() != null) {
            Plant plant = tile.getPlant();
            plant.setCurrentHealth(0);
            tile.setPlant(null);
        }

        cooldownForFishing = Math.max(0, cooldownForFishing - tickCount);
        if (cooldownForFishing == 0) {
            Tile sourceTile = getTheClosestTileWithPlant(map);
            if (sourceTile != null) {
                Plant plant = sourceTile.getPlant();
                sourceTile.setPlant(null);
                Tile destinationTile = map.getTile(sourceTile.getX() + 1, y);
                if (destinationTile != null) {
                    destinationTile.setPlant(plant);
                }
                cooldownForFishing = 50;
            }
        }
    }

    private Tile getTheClosestTileWithPlant(GameMap map) {
        for (int i = map.getCols() - 1; i >= 1; i--) {
            Tile current = map.getTile(i, y);
            Tile next = map.getTile(i + 1, y);
            if (current != null && current.getPlant() != null
                    && next != null && next.getPlant() == null) {
                return current;
            }
        }
        return null;
    }

    // ==================== غواص (Snorkel Zombie) ====================

    /**
     * تا وقتی زیر آب است فقط lobber می‌تواند به آن آسیب بزند؛ برای خوردن گیاه
     * به سطح آب می‌آید و در آن لحظه آسیب‌پذیر می‌شود.
     */
    private void snorkelTick(GameSession gameSession) {
        GameMap map = gameSession.getGameMap();
        Tile tile = map.getTile((int) x, y);
        if (tile == null) {
            return;
        }
        if (isAttacking(gameSession)) {
            submerged = false;
        } else {
            submerged = tile.isWater();
        }
    }

    public boolean isSubmerged() {
        return submerged;
    }

    // ==================== اختاپوس‌پرت‌کن (Octopus Zombie) ====================

    /**
     * هر چند ثانیه یک‌بار، به نزدیک‌ترین گیاه سطر خودش اختاپوس پرتاب می‌کند
     * که اثری مشابه یخ‌زدگی روی گیاه ایجاد می‌کند.
     */
    private void octopusTick(GameSession gameSession) {
        octopusCooldown--;
        if (octopusCooldown > 0) {
            return;
        }
        octopusCooldown = 20;
        Plant target = getClosestPlantInLane(gameSession.getGameMap());
        if (target != null && !target.hasEffect(PlantEffect.OCTOPUSED)) {
            target.addEffect(PlantEffect.OCTOPUSED, Integer.MAX_VALUE);
        }
    }

    // ==================== جادوگر (Wizard Zombie) ====================

    /**
     * هر چند ثانیه یک‌بار، یک گیاه تصادفی روی زمین را به گربه تبدیل می‌کند
     * (نه حمله می‌کند نه خورده می‌شود). با مرگ جادوگر، اثر از بین می‌رود (در onDeath).
     */
    private void wizardTick(GameSession gameSession) {
        wizardCooldown--;
        if (wizardCooldown > 0) {
            return;
        }
        wizardCooldown = 30;
        GameMap map = gameSession.getGameMap();
        Tile[][] tiles = map.getTiles();
        if (tiles == null) {
            return;
        }
        List<Plant> candidates = new ArrayList<>();
        for (Tile[] row : tiles) {
            if (row == null) {
                continue;
            }
            for (Tile tile : row) {
                if (tile != null && tile.getPlant() != null && !tile.getPlant().hasEffect(PlantEffect.WIZARDED)) {
                    candidates.add(tile.getPlant());
                }
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        Plant chosen = candidates.get(RandomUtil.nextInt(0, candidates.size() - 1));
        chosen.addEffect(PlantEffect.WIZARDED, Integer.MAX_VALUE);
        wizardedPlants.add(chosen);
    }

    // ==================== پادشاه (King Zombie) ====================

    /**
     * در راست‌ترین ستون بدون حرکت می‌ماند و هر چند ثانیه یک‌بار یکی از زامبی‌های
     * معمولی نزدیک خود را به شوالیه تبدیل می‌کند (با دادن کلاه‌خود و شانه‌بند).
     */
    private void kingTick(GameSession gameSession) {
        kingCooldown--;
        if (kingCooldown > 0) {
            return;
        }
        kingCooldown = 50;
        List<Zombie> zombies = gameSession.getActiveZombies();
        if (zombies == null) {
            return;
        }
        for (Zombie other : zombies) {
            boolean sameLane = other.getY() == y;
            boolean nearby = Math.abs(other.getX() - x) <= 2;
            if (other != this && other.isAlive() && other.getType() == ZombieType.NORMAL
                    && sameLane && nearby && !other.hasArmor(ArmorType.HELMET)) {
                other.getArmors().put(ArmorType.HELMET, 1600);
                other.getArmors().put(ArmorType.SHOULDER_ARMOR, 1600);
                break;
            }
        }
    }

    // ==================== مینی‌گیم Zombotany ====================

    /** زامبی تیرانداز مانند peashooter به جلو تیر شلیک می‌کند */
    private void zombotanyPeashooterTick(GameSession gameSession) {
        peashooterCooldown--;
        if (peashooterCooldown > 0) {
            return;
        }
        peashooterCooldown = 14;
        List<Projectile> projectiles = gameSession.getActiveProjectiles();
        if (projectiles != null) {
            Projectile projectile = new Projectile(ProjectileType.NORMAL, x, y, damagePerSecond);
            projectiles.add(projectile);
        }
    }

    /** زامبی فلفل، اگر تا 10 ثانیه نابود نشود، کل سطر خودش را آتش می‌زند و خودش هم از بین می‌رود */
    private void zombotanyJalapenoTick(GameSession gameSession) {
        ticksSinceSpawn++;
        if (ticksSinceSpawn < JALAPENO_FUSE_TICKS || !isAlive()) {
            return;
        }
        GameMap map = gameSession.getGameMap();
        Tile[] row = map.getRow(y);
        if (row != null) {
            for (Tile tile : row) {
                if (tile != null && tile.getPlant() != null) {
                    tile.getPlant().setCurrentHealth(0);
                    tile.setPlant(null);
                }
            }
        }
        currentHealth = 0;
    }

    /** زامبی کدو با رسیدن به گیاه، هم خودش و هم گیاه را از بین می‌برد */
    private void zombotanySquashTick(GameSession gameSession) {
        GameMap map = gameSession.getGameMap();
        Tile tile = map.getTile((int) x, y);
        if (tile != null && tile.getPlant() != null) {
            tile.getPlant().setCurrentHealth(0);
            tile.setPlant(null);
            currentHealth = 0;
        }
    }

    // ==================== زامبی تولید خورشید (I, Zombie) ====================

    /**
     * برای زامبی‌ها خورشید تولید می‌کند؛ نرخ تولید با گذر زمان بیشتر می‌شود.
     */
    private void sunProducerTick(GameSession gameSession) {
        sunProducerCooldown--;
        if (sunProducerCooldown > 0) {
            return;
        }
        double elapsedSeconds = gameSession.getElapsedSeconds();
        sunProducerCooldown = (int) Math.max(30, 100 - elapsedSeconds);
        List<Sun> suns = gameSession.getActiveSuns();
        if (suns != null) {
            Sun sun = new Sun(SunType.NORMAL, (int) x, y, gameSession.getCurrentTick());
            sun.setFromPlant(true);
            sun.setLanded(true);
            suns.add(sun);
        }
    }

    // ==================== ابزار مشترک ====================

    /** بررسی می‌کند آیا در این خانه زامبی هیپنوتیزم‌شده‌ای (به جز خود این زامبی) وجود دارد */
    private boolean isHypnoZombieOnTile(Tile tile) {
        List<Zombie> zombiesOnTile = tile.getZombiesOnTile();
        if (zombiesOnTile == null) {
            return false;
        }
        for (Zombie other : zombiesOnTile) {
            if (other != this && other.hasEffect(ZombieEffect.HYPNOTIZED)) {
                return true;
            }
        }
        return false;
    }

    /** آیا این زامبی (چتردار) ضربات lobber را دفع می‌کند */
    public boolean blocksLobberDamage() {
        return type == ZombieType.PARASOL_ZOMBIE;
    }

    /** آیا این زامبی (دودو سوار) از روی موانع پرواز می‌کند */
    public boolean isFlying() {
        return type == ZombieType.DODO_RIDER;
    }

    @Override
    public String getDescription() {
        return "Zombie: A basic zombie. Not very fast, not very tough.";
    }
}
