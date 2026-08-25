package com.pvz2.model;

import com.pvz2.model.enums.PlantType;
import com.pvz2.model.enums.ZombieType;
import com.pvz2.util.RandomUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * وضعیت مینی‌گیم «کوزه‌شکنی» (Vasebreaker) برای فاز گرافیک.
 *
 * <p>این کلاس فقط <b>state</b> است (لایه Model) و هیچ وابستگی‌ای به کنسول یا
 * گرافیک ندارد؛ در {@link GameSession#setMinigameState(Object)} ذخیره می‌شود و
 * {@code GameService} منطق آن را پیش می‌برد.
 *
 * <p><b>سه نوع کوزه</b> (طبق داک — DT3):
 * <ul>
 *   <li>{@link VaseKind#GARGANTUAR} (بنفش): همیشه یک زامبی غول‌پیکر آزاد می‌کند.</li>
 *   <li>{@link VaseKind#PLANT} (سبز): همیشه یک بذر گیاه می‌دهد که بازیکن می‌تواند بکارد.</li>
 *   <li>{@link VaseKind#RANDOM} (نارنجی): خروجی تصادفی — زامبی یا گیاه.</li>
 * </ul>
 * محتوای هر کوزه هنگام ساخت pre-roll می‌شود؛ رنگِ کوزه (kind) از قبل دیده می‌شود
 * ولی محتوای دقیقِ کوزه نارنجی فقط پس از شکستن مشخص می‌گردد.
 */
public class VasebreakerState {

    /** رنگ/نوع ظاهری کوزه — قبل از شکستن دیده می‌شود. */
    public enum VaseKind { PLANT, GARGANTUAR, RANDOM }

    /** یک کوزه روی خانه‌ای از زمین (مختصات ۱-based مطابق فاز ۱). */
    public static final class Vase {
        public final int col;
        public final int row;
        public final VaseKind kind;
        public boolean broken;
        /** اگر محتوا زامبی باشد، غیر-null. */
        public ZombieType zombieContent;
        /** اگر محتوا بذر گیاه باشد، غیر-null. */
        public PlantType plantContent;

        public Vase(int col, int row, VaseKind kind) {
            this.col = col;
            this.row = row;
            this.kind = kind;
        }
    }

    /** استخر گیاهانی که ممکن است داخل کوزه‌ها باشند (دفاعی + تهاجمی). */
    private static final PlantType[] PLANT_POOL = {
        PlantType.PEASHOOTER, PlantType.SNOW_PEA, PlantType.WALL_NUT,
        PlantType.POTATO_MINE, PlantType.CABBAGE_PULT, PlantType.REPEATER
    };

    /** استخر زامبی‌های معمولیِ کوزه‌ها (به‌جز غول‌پیکر). */
    private static final ZombieType[] ZOMBIE_POOL = {
        ZombieType.NORMAL, ZombieType.CONEHEAD, ZombieType.BUCKETHEAD
    };

    private final int level;
    private final List<Vase> vases = new ArrayList<>();
    /** بذرهای برداشت‌شده که قابل کاشت‌اند — کلید: نوع گیاه، مقدار: تعداد. */
    private final Map<PlantType, Integer> inventory = new LinkedHashMap<>();

    public VasebreakerState(int level, int rows, int firstVaseCol, int lastVaseCol) {
        this.level = level;
        generate(rows, firstVaseCol, lastVaseCol);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ساخت اولیه کوزه‌ها
    // ─────────────────────────────────────────────────────────────────────────

    private void generate(int rows, int firstVaseCol, int lastVaseCol) {
        for (int r = 1; r <= rows; r++) {
            for (int c = firstVaseCol; c <= lastVaseCol; c++) {
                Vase vase = new Vase(c, r, rollKind(c, firstVaseCol, lastVaseCol));
                rollContent(vase);
                vases.add(vase);
            }
        }
        ensureStarterPlants(firstVaseCol, rows);
    }

    private VaseKind rollKind(int col, int firstVaseCol, int lastVaseCol) {
        // ستون سمت راست در سطوح ۲ و ۳ می‌تواند غول‌پیکر داشته باشد.
        if (level >= 2 && col == lastVaseCol && RandomUtil.chance(0.5)) {
            return VaseKind.GARGANTUAR;
        }
        double roll = RandomUtil.nextDouble();
        if (roll < 0.35) return VaseKind.PLANT;
        if (level >= 2 && roll < 0.45) return VaseKind.GARGANTUAR;
        return VaseKind.RANDOM;
    }

    private void rollContent(Vase vase) {
        switch (vase.kind) {
            case PLANT:
                vase.plantContent = randomPlant();
                break;
            case GARGANTUAR:
                vase.zombieContent = ZombieType.GARGANTUAR;
                break;
            case RANDOM:
            default:
                // نارنجی: ۵۵٪ زامبی، ۴۵٪ گیاه
                if (RandomUtil.chance(0.55)) {
                    vase.zombieContent = ZOMBIE_POOL[RandomUtil.nextInt(ZOMBIE_POOL.length)];
                } else {
                    vase.plantContent = randomPlant();
                }
                break;
        }
    }

    /** تضمین اینکه چند بذر گیاه در چپ‌ترین ستونِ کوزه‌ها باشد تا مرحله قابل بازی بماند. */
    private void ensureStarterPlants(int firstVaseCol, int rows) {
        int guaranteed = 0;
        for (Vase v : vases) {
            if (v.col == firstVaseCol && v.plantContent != null) guaranteed++;
        }
        if (guaranteed >= 2) return;
        // تبدیل چند کوزه‌ی چپ‌ترین ستون به گیاه
        for (Vase v : vases) {
            if (guaranteed >= 2) break;
            if (v.col == firstVaseCol && v.plantContent == null) {
                v.zombieContent = null;
                v.plantContent = randomPlant();
                guaranteed++;
            }
        }
    }

    private PlantType randomPlant() {
        return PLANT_POOL[RandomUtil.nextInt(PLANT_POOL.length)];
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  دسترسی و منطق
    // ─────────────────────────────────────────────────────────────────────────

    public List<Vase> getVases() { return vases; }

    /** کوزه‌ی نشکسته در خانه‌ی مشخص (یا null). */
    public Vase unbrokenVaseAt(int col, int row) {
        for (Vase v : vases) {
            if (v.col == col && v.row == row && !v.broken) return v;
        }
        return null;
    }

    public boolean hasUnbrokenVaseAt(int col, int row) {
        return unbrokenVaseAt(col, row) != null;
    }

    public boolean allBroken() {
        for (Vase v : vases) {
            if (!v.broken) return false;
        }
        return true;
    }

    // ─── انبار بذر ─────────────────────────────────────────────────────────────

    public Map<PlantType, Integer> getInventory() { return inventory; }

    public void addSeed(PlantType type) {
        inventory.merge(type, 1, Integer::sum);
    }

    public boolean hasSeed(PlantType type) {
        return inventory.getOrDefault(type, 0) > 0;
    }

    /** یک بذر از انبار مصرف می‌کند؛ false اگر موجودی نبود. */
    public boolean consumeSeed(PlantType type) {
        int count = inventory.getOrDefault(type, 0);
        if (count <= 0) return false;
        if (count == 1) inventory.remove(type);
        else inventory.put(type, count - 1);
        return true;
    }

    /** انبار بذرها به‌صورت لیستِ مسطح (هر بذر یک‌بار به‌ازای هر واحد) — برای نوار بذر. */
    public List<PlantType> flattenedInventory() {
        List<PlantType> out = new ArrayList<>();
        for (Map.Entry<PlantType, Integer> e : inventory.entrySet()) {
            for (int i = 0; i < e.getValue(); i++) out.add(e.getKey());
        }
        return out;
    }

    public int remainingVaseCount() {
        int n = 0;
        for (Vase v : vases) if (!v.broken) n++;
        return n;
    }
}
