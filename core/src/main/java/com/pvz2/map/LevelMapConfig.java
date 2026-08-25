package com.pvz2.map;

import com.pvz2.model.Level;
import com.pvz2.model.enums.ChapterType;
import com.pvz2.model.enums.LevelType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * تنظیمات نقشه برای یک مرحله خاص.
 *
 * تعیین می‌کند:
 *   - کدام گروه فصلی فعال شود
 *   - کدام گروه‌های special اضافه شوند
 *   - آیا دوربین باید به حالت extended برود
 *   - seed برای تنوع تصادفی کاشی‌ها
 *
 * ساخت:
 *   LevelMapConfig config = LevelMapConfig.fromLevel(level);
 *   // یا
 *   LevelMapConfig config = new LevelMapConfig.Builder()
 *       .chapter(ChapterType.ANCIENT_EGYPT)
 *       .addSpecial("CONVEYOR_BELT")
 *       .extendedCamera(true)
 *       .build();
 *
 * <p><b>نکته پیاده‌سازی (اتصال به فاز ۱):</b> نسخه اولیه این کلاس فرض کرده بود
 * {@code com.pvz2.model.Level} متدهایی مثل {@code getId()}, {@code isConveyorBelt()},
 * {@code isSandstorm()}, {@code isTidalWave()}, {@code isTotalFreeze()},
 * {@code isNightMode()}, {@code hasRailMechanic()}, {@code getMinigameType()} دارد.
 * {@code Level} واقعی در فاز ۱ این متدها را ندارد — فقط {@link Level#getLevelType()}
 * و {@link Level#getChapter()} را دارد. متد {@link #fromLevel(Level)} پایین بازنویسی شده
 * تا کاملاً بر اساس {@link LevelType} کار کند:
 * <ul>
 *   <li>{@code CONVEYOR_BELT} → special {@code CONVEYOR_BELT} + دوربین extended</li>
 *   <li>{@code NIGHT_OPS}     → special {@code NIGHT_MODE}</li>
 *   <li>{@code VASEBREAKER}/{@code WALLNUT_BOWLING}/{@code I_ZOMBIE}/{@code BEGHOULED}
 *       → مینی‌گیم (گروه {@code mg_*})</li>
 * </ul>
 * {@code SANDSTORM}, {@code TIDAL_WAVE}, {@code TOTAL_FREEZE} هیچ معادل ایستایی در
 * فاز ۱ ندارند (نه در {@link LevelType} و نه در {@link Level})، پس اینجا فعال نمی‌شوند.
 * نزدیک‌ترین مفاهیم پویای فاز ۱ — گردباد مصر ({@code GameStateSnapshot.egyptTornadoActive})
 * و باد یخی غار ({@code GameStateSnapshot.frostbiteWindRows}) — روی‌دادهای per-tick هستند،
 * نه ویژگی ایستای یک مرحله، بنابراین اینجا (که فقط یک‌بار در شروع مرحله اجرا می‌شود) جای
 * مناسبی برای آن‌ها نیست. اگر تیم بخواهد گروه‌های {@code sp_sandstorm}/{@code sp_frozen} را
 * به این پرچم‌های runtime وصل کند، باید در {@code GameScreen} هر فریم روی
 * {@code GameMap.getTiledMap()} کار کند (نمونه‌اش در TILED_MAP_GUIDE.md نیامده، باید دستی
 * اضافه شود).
 */
public final class LevelMapConfig {

    private final ChapterType   chapter;
    private final List<String>  specialIds;     // گروه‌های sp_* که باید فعال شوند
    private final String        minigameId;     // گروه mg_* (یا null)
    private final boolean       extendedCamera;
    private final long          variantSeed;

    private LevelMapConfig(Builder b) {
        this.chapter        = b.chapter;
        this.specialIds     = Collections.unmodifiableList(new ArrayList<>(b.specialIds));
        this.minigameId     = b.minigameId;
        this.extendedCamera = b.extendedCamera;
        this.variantSeed    = b.variantSeed;
    }

    // ─── Factory از Level (فاز ۱ واقعی) ──────────────────────

    /** انواع LevelType که در واقع مینی‌گیم هستند (طبق فاز ۱، نه یک enum جدا). */
    private static final List<LevelType> MINIGAME_TYPES = Arrays.asList(
            LevelType.VASEBREAKER, LevelType.WALLNUT_BOWLING,
            LevelType.I_ZOMBIE, LevelType.BEGHOULED);

    /**
     * تبدیل خودکار یک Level به LevelMapConfig.
     * منطق: {@link Level#getLevelType()} و {@link Level#getChapter()} را می‌خواند و
     * تصمیم می‌گیرد کدام لایه‌ها فعال شوند.
     *
     * @param level مرحله جاری — اگر null باشد، تنظیمات پیش‌فرض (مصر، بدون special) برمی‌گردد
     */
    public static LevelMapConfig fromLevel(Level level) {
        Builder b = new Builder();
        if (level == null) return b.build();

        ChapterType chapter = level.getChapter() != null
                ? level.getChapter() : ChapterType.ANCIENT_EGYPT;
        b.chapter(chapter);

        LevelType lt = level.getLevelType();

        // seed = هش ترکیب فصل + شماره مرحله + نوع مرحله
        // (Level.getId() وجود ندارد؛ این ترکیب هم پایدار است هم به ازای هر مرحله یکتا)
        String seedKey = chapter.name() + "#" + level.getLevelNumber() + "#" + lt;
        b.variantSeed(seedKey.hashCode());

        boolean hasRail = false;
        if (lt == LevelType.CONVEYOR_BELT) {
            b.addSpecial("CONVEYOR_BELT");
            hasRail = true;
        }
        if (lt == LevelType.NIGHT_OPS) {
            b.addSpecial("NIGHT_MODE");
        }

        b.extendedCamera(hasRail);

        if (MINIGAME_TYPES.contains(lt)) {
            b.minigame(lt.name());
        }

        return b.build();
    }

    // ─── Getters ────────────────────────────────────────────

    public ChapterType  getChapter()        { return chapter; }
    public List<String> getSpecialIds()     { return specialIds; }
    public String       getMinigameId()     { return minigameId; }
    public boolean      isExtendedCamera()  { return extendedCamera; }
    public long         getVariantSeed()    { return variantSeed; }
    public boolean      isMinigame()        { return minigameId != null && !minigameId.isEmpty(); }
    public boolean      hasSpecial(String id){ return specialIds.contains(id); }

    /**
     * نام Group Layer فصل در TMX.
     * مثال: ANCIENT_EGYPT → "ch_egypt"
     */
    public String getChapterGroupName() {
        if (chapter == null) return "shared";
        switch (chapter) {
            case ANCIENT_EGYPT:   return "ch_egypt";
            case FROSTBITE_CAVES: return "ch_frost";
            case BIG_WAVE_BEACH:  return "ch_beach";
            case DARK_AGES:       return "ch_dark";
            default:              return "ch_egypt";
        }
    }

    /**
     * نام Group Layer مینی‌گیم.
     * گروه‌ها در world.tmx با نام بزرگ و آندرلاین ذخیره شده‌اند
     * (`mg_VASEBREAKER`, `mg_WALLNUT_BOWLING`, `mg_I_ZOMBIE`, `mg_BEGHOULED`,
     * `mg_ZOMBOTANY`)، پس آندرلاین را حذف نمی‌کنیم و همان نام enum را حفظ می‌کنیم
     * (تطبیق در MapLayerManager بی‌توجه به بزرگی/کوچکی حروف است).
     */
    public String getMinigameGroupName() {
        if (minigameId == null) return null;
        return "mg_" + minigameId.toUpperCase();
    }

    /**
     * نام Group Layer یک special.
     * مثال: CONVEYOR_BELT → "sp_conveyor"
     */
    public String getSpecialGroupName(String specialId) {
        if (specialId == null) return null;
        switch (specialId.toUpperCase()) {
            case "CONVEYOR_BELT": return "sp_conveyor";
            case "SANDSTORM":     return "sp_sandstorm";
            case "TIDAL_WAVE":    return "sp_tidal";
            case "TOTAL_FREEZE":  return "sp_frozen";
            case "NIGHT_MODE":    return "sp_night";
            default:              return "sp_" + specialId.toLowerCase();
        }
    }

    // ─── Builder ────────────────────────────────────────────

    public static final class Builder {
        private ChapterType chapter    = ChapterType.ANCIENT_EGYPT;
        private final List<String> specialIds = new ArrayList<>();
        private String minigameId     = null;
        private boolean extendedCamera = false;
        private long variantSeed       = 0L;

        public Builder chapter(ChapterType c)      { this.chapter = c;             return this; }
        public Builder addSpecial(String id)        { this.specialIds.add(id);      return this; }
        public Builder specials(String... ids)      { specialIds.addAll(Arrays.asList(ids)); return this; }
        public Builder minigame(String id)          { this.minigameId = id;         return this; }
        public Builder extendedCamera(boolean e)    { this.extendedCamera = e;      return this; }
        public Builder variantSeed(long s)          { this.variantSeed = s;         return this; }
        public LevelMapConfig build()               { return new LevelMapConfig(this); }
    }
}
