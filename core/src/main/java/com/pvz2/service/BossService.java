package com.pvz2.service;

import com.pvz2.model.Boss;
import com.pvz2.model.Boss.AbilityType;
import com.pvz2.model.GameSession;
import com.pvz2.model.enums.ChapterType;
import com.pvz2.model.enums.TileType;
import com.pvz2.model.enums.ZombieEffect;
import com.pvz2.model.enums.ZombieType;
import com.pvz2.model.plants.Plant;
import com.pvz2.model.tiles.Tile;
import com.pvz2.model.zombies.Zombie;
import com.pvz2.model.zombies.ZombieFactory;
import com.pvz2.util.RandomUtil;
import com.pvz2.view.ConsoleView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * سرویسِ AIِ رئیس (Zomboss) — هر تیک برای مراحلِ
 * {@link com.pvz2.model.enums.LevelType#BOSS} فراخوانی می‌شود.
 *
 * <p><b>رفتارِ اختصاصیِ هر فصل</b> (مطابقِ داکیومنت):
 * <ul>
 *   <li>مصر: موشک (نابودیِ گیاه + ۲ سنگ‌قبر) + شارژِ سریع (نابودیِ ۲ سطر) + احضار — متحرک.</li>
 *   <li>یخ: موشکِ یخ + بادِ یخی (۲ سطر) + یخ‌زدنِ ستون (+زامبیِ یخ‌زده) — <b>ثابت</b>، بدون احضار.</li>
 *   <li>ساحل: بچه‌کوسه (خوردنِ گیاهِ آبی) + توربین (نابودیِ ۲ سطر).</li>
 *   <li>تاریک: توپِ آتش + احضار.</li>
 * </ul>
 */
public class BossService {

    private final ConsoleView view;

    // ─── زمان‌بندی (تیک؛ ۱۰ تیک = ۱ ثانیه) ───────────────────────────────────────
    private static final int ENTER_TICKS   = 26;
    private static final int DYING_TICKS    = 16;
    private static final int MOVING_TICKS   = 7;
    private static final int ATTACK_ANIM_TICKS = 10;
    private static final int SUMMON_ANIM_TICKS = 10;

    private static final int ATTACK_INCOMING_TICKS = 16;
    private static final int ATTACK_IMPACT_TICKS   = 14;
    /** GZ3: مدتِ گیج‌شدنِ زامباس بعد از تمام‌شدنِ هر بخشِ سلامتی (۳٫۵ ثانیه). */
    private static final int STUN_TICKS = 35;

    private static final int GRACE_ATTACK = 170;
    private static final int GRACE_SPAWN  = 150;

    private static final int[] ATTACK_INTERVAL = { 200, 150, 105 };
    private static final int[] SPAWN_INTERVAL  = { 320, 240, 165 };
    private static final int[] MOVE_INTERVAL   = { 95, 80, 65 };

    private static final int MINION_CAP = 6;

    public BossService(ConsoleView view) {
        this.view = view;
    }

    // ════════════════════════════════════════════════════════════
    //  SETUP
    // ════════════════════════════════════════════════════════════

    public void setup(GameSession session) {
        ChapterType chapter = session.getGameMap().getChapter();
        int cols = session.getGameMap().getCols();
        int rows = session.getGameMap().getRows();
        Boss boss = new Boss(chapter, healthForChapter(chapter), cols + 0.5, 1);

        // پیکربندیِ اختصاصیِ فصل
        if (chapter == ChapterType.FROSTBITE_CAVES) {
            boss.setCanMoveLanes(false);   // ماموتِ ثابت که کلِ ردیف‌ها را اشغال می‌کند
            boss.setLane(1);
            boss.setLaneSpan(rows);
        } else {
            boss.setCanMoveLanes(true);
            boss.setLane(Math.max(1, (rows + 1) / 2));
            boss.setLaneSpan(2);
        }
        boss.setTargetLane(boss.getLane());
        boss.setState(Boss.State.ENTERING);
        boss.setAttackTimer(GRACE_ATTACK);
        boss.setSpawnTimer(GRACE_SPAWN);
        boss.setMoveTimer(MOVE_INTERVAL[0]);
        session.setBoss(boss);
        view.printRaw("[31m[1m👹 ZOMBOSS has appeared! Defeat all 3 health segments![0m");
    }

    private int healthForChapter(ChapterType chapter) {
        if (chapter == null) return 2400;
        switch (chapter) {
            case FROSTBITE_CAVES: return 3000;
            case DARK_AGES:       return 3600;
            case BIG_WAVE_BEACH:  return 2600;
            case ANCIENT_EGYPT:
            default:              return 2400;
        }
    }

    /** آیا این فصل زامبیِ عادیِ دوره‌ای احضار می‌کند (مصر/تاریک آری، یخ/ساحل خیر). */
    private boolean chapterSummons(ChapterType chapter) {
        return chapter == ChapterType.ANCIENT_EGYPT || chapter == ChapterType.DARK_AGES;
    }

    // ════════════════════════════════════════════════════════════
    //  TICK
    // ════════════════════════════════════════════════════════════

    public void tick(GameSession session) {
        Boss boss = session.getBoss();
        if (boss == null) return;
        boss.addStateTime(0.1);

        updateAttacks(boss, session);
        updateChargeAnim(boss);

        // GZ3: با تمام‌شدنِ هر بخشِ سلامتی (عبور به ⅔ یا ⅓)، زامباس برای مدتی گیج
        // می‌شود و کاری نمی‌کند. تشخیص با کاهشِ segmentsRemaining انجام می‌شود.
        int seg = boss.segmentsRemaining();
        if (seg < boss.getLastSegments() && seg > 0
                && boss.getState() != Boss.State.ENTERING
                && boss.getState() != Boss.State.DYING
                && boss.getState() != Boss.State.DEAD
                && boss.getState() != Boss.State.STUNNED) {
            boss.setState(Boss.State.STUNNED);
            boss.setCurrentAbility(null);
            view.printRaw("[33m[1m😵 ZOMBOSS is dazed![0m");
        }
        boss.setLastSegments(seg);

        switch (boss.getState()) {
            case ENTERING:
                if (boss.getStateTime() >= ENTER_TICKS / 10.0) boss.setState(Boss.State.IDLE);
                return;
            case STUNNED:
                if (boss.isHealthDepleted()) { boss.setState(Boss.State.DYING); return; }
                if (boss.getStateTime() >= STUN_TICKS / 10.0) boss.setState(Boss.State.IDLE);
                return; // گیج: بدونِ حمله/حرکت/احضار
            case DYING:
                if (boss.getStateTime() >= DYING_TICKS / 10.0) boss.setState(Boss.State.DEAD);
                return;
            case DEAD:
                return;
            case MOVING:
                if (boss.getStateTime() >= MOVING_TICKS / 10.0) boss.setState(Boss.State.IDLE);
                break;
            case ATTACKING:
                if (boss.getStateTime() >= ATTACK_ANIM_TICKS / 10.0) {
                    boss.setState(Boss.State.IDLE);
                    boss.setCurrentAbility(null);
                }
                break;
            case SUMMONING:
                if (boss.getStateTime() >= SUMMON_ANIM_TICKS / 10.0) boss.setState(Boss.State.IDLE);
                break;
            default:
                break;
        }

        if (boss.isHealthDepleted()) {
            boss.setState(Boss.State.DYING);
            return;
        }

        int phaseIdx = Math.max(0, Math.min(2, boss.phase() - 1));
        tickTimers(boss);

        if (boss.getState() == Boss.State.IDLE) {
            if (boss.getAttackTimer() <= 0) {
                launchAbility(boss, session, pickAbility(boss.getChapter()));
                boss.setAttackTimer(ATTACK_INTERVAL[phaseIdx]);
                boss.setState(Boss.State.ATTACKING);
            } else if (chapterSummons(boss.getChapter()) && boss.getSpawnTimer() <= 0) {
                if (activeMinionCount(session) < MINION_CAP) {
                    spawnMinion(session, boss.getChapter());
                    boss.setSpawnTimer(SPAWN_INTERVAL[phaseIdx]);
                    boss.setState(Boss.State.SUMMONING);
                } else {
                    boss.setSpawnTimer(30);
                }
            } else if (boss.canMoveLanes() && boss.getMoveTimer() <= 0) {
                startMove(boss, session);
                boss.setMoveTimer(MOVE_INTERVAL[phaseIdx]);
                boss.setState(Boss.State.MOVING);
            }
        }
    }

    private void tickTimers(Boss boss) {
        boss.setAttackTimer(boss.getAttackTimer() - 1);
        boss.setSpawnTimer(boss.getSpawnTimer() - 1);
        boss.setMoveTimer(boss.getMoveTimer() - 1);
    }

    private int activeMinionCount(GameSession session) {
        int n = 0;
        for (Zombie z : session.getActiveZombies()) if (z.isAlive()) n++;
        return n;
    }

    // ════════════════════════════════════════════════════════════
    //  MOVEMENT (فقط فصل‌های متحرک)
    // ════════════════════════════════════════════════════════════

    private void startMove(Boss boss, GameSession session) {
        int rows = session.getGameMap().getRows();
        if (rows <= 2) return;
        int newLane;
        do { newLane = RandomUtil.between(1, rows - 1); } while (newLane == boss.getLane());
        boss.setTargetLane(newLane);
        boss.setLane(newLane);
    }

    /** جابه‌جاییِ شارژ (لومیدنِ رئیس به جلو هنگامِ EGYPT_CHARGE). */
    private void updateChargeAnim(Boss boss) {
        Boss.BossAttack charge = null;
        for (Boss.BossAttack a : boss.getAttacks()) {
            if (a.type == AbilityType.EGYPT_CHARGE) { charge = a; break; }
        }
        if (charge == null) { boss.setChargeOffset(0f); return; }
        if (charge.phase == Boss.BossAttack.Phase.INCOMING) {
            boss.setChargeOffset(-2.6f * charge.progress);          // به جلو
        } else if (charge.phase == Boss.BossAttack.Phase.IMPACT) {
            boss.setChargeOffset(-2.6f * charge.timer / ATTACK_IMPACT_TICKS); // بازگشت
        } else {
            boss.setChargeOffset(0f);
        }
    }

    // ════════════════════════════════════════════════════════════
    //  ABILITY SELECTION + LAUNCH
    // ════════════════════════════════════════════════════════════

    private AbilityType pickAbility(ChapterType chapter) {
        if (chapter == null) return AbilityType.EGYPT_MISSILE;
        switch (chapter) {
            case FROSTBITE_CAVES: {
                double r = RandomUtil.nextDouble();
                if (r < 0.45) return AbilityType.ICE_MISSILE;
                if (r < 0.75) return AbilityType.ICE_WIND;
                return AbilityType.ICE_FREEZE_COLUMN;
            }
            case BIG_WAVE_BEACH:
                return RandomUtil.chance(0.45) ? AbilityType.BEACH_TURBINE : AbilityType.BEACH_SHARK;
            case DARK_AGES:
                return AbilityType.DARK_FIREBALL;
            case ANCIENT_EGYPT:
            default:
                return RandomUtil.chance(0.3) ? AbilityType.EGYPT_CHARGE : AbilityType.EGYPT_MISSILE;
        }
    }

    private void launchAbility(Boss boss, GameSession session, AbilityType ability) {
        boss.setCurrentAbility(ability);
        int rows = session.getGameMap().getRows();
        int cols = session.getGameMap().getCols();
        Boss.BossAttack atk;
        switch (ability) {
            case EGYPT_CHARGE:
            case BEACH_TURBINE: {
                int r1 = boss.getLane();
                int r2 = boss.secondLane(rows);
                atk = new Boss.BossAttack(ability, 1, r1, ATTACK_INCOMING_TICKS);
                atk.targetRow2 = r2;
                break;
            }
            case ICE_WIND: {
                int r1 = RandomUtil.between(1, rows);
                int r2 = RandomUtil.between(1, rows);
                atk = new Boss.BossAttack(ability, 1, r1, ATTACK_INCOMING_TICKS);
                atk.targetRow2 = r2;
                break;
            }
            case ICE_FREEZE_COLUMN: {
                int col = pickColumnWithPlants(session);
                atk = new Boss.BossAttack(ability, col, -1, ATTACK_INCOMING_TICKS);
                atk.wholeColumn = true;
                break;
            }
            case BEACH_SHARK: {
                int[] cell = pickWaterPlantCell(session);
                atk = new Boss.BossAttack(ability, cell[0], cell[1], ATTACK_INCOMING_TICKS);
                break;
            }
            case EGYPT_MISSILE:
            case ICE_MISSILE:
            case DARK_FIREBALL:
            default: {
                int r = pickTargetLane(session, boss.getLane());
                int c = pickTargetColumn(session, r);
                atk = new Boss.BossAttack(ability, c, r, ATTACK_INCOMING_TICKS);
                break;
            }
        }
        boss.getAttacks().add(atk);
    }

    // ── انتخابِ هدف ──────────────────────────────────────────────────────────────

    private int pickTargetLane(GameSession session, int fallbackLane) {
        int rows = session.getGameMap().getRows();
        int cols = session.getGameMap().getCols();
        List<Integer> lanes = new ArrayList<>();
        for (int r = 1; r <= rows; r++) {
            for (int c = 1; c <= cols; c++) {
                Tile t = session.getGameMap().getTile(c, r);
                if (t != null && t.getPlant() != null) { lanes.add(r); break; }
            }
        }
        return lanes.isEmpty() ? fallbackLane : lanes.get(RandomUtil.between(0, lanes.size() - 1));
    }

    private int pickTargetColumn(GameSession session, int row) {
        int cols = session.getGameMap().getCols();
        List<Integer> plantCols = new ArrayList<>();
        for (int c = 1; c <= cols; c++) {
            Tile t = session.getGameMap().getTile(c, row);
            if (t != null && t.getPlant() != null) plantCols.add(c);
        }
        if (plantCols.isEmpty()) return Math.max(1, cols / 2);
        return plantCols.get(RandomUtil.between(0, plantCols.size() - 1));
    }

    private int pickColumnWithPlants(GameSession session) {
        int rows = session.getGameMap().getRows();
        int cols = session.getGameMap().getCols();
        List<Integer> cs = new ArrayList<>();
        for (int c = 1; c <= cols; c++) {
            for (int r = 1; r <= rows; r++) {
                Tile t = session.getGameMap().getTile(c, r);
                if (t != null && t.getPlant() != null) { cs.add(c); break; }
            }
        }
        return cs.isEmpty() ? RandomUtil.between(1, Math.max(1, cols / 2)) : cs.get(RandomUtil.between(0, cs.size() - 1));
    }

    /** خانه‌ی گیاهِ روی آب (برای بچه‌کوسه)؛ اگر نبود، هر گیاهی. */
    private int[] pickWaterPlantCell(GameSession session) {
        int rows = session.getGameMap().getRows();
        int cols = session.getGameMap().getCols();
        List<int[]> water = new ArrayList<>();
        List<int[]> any = new ArrayList<>();
        for (int c = 1; c <= cols; c++) {
            for (int r = 1; r <= rows; r++) {
                Tile t = session.getGameMap().getTile(c, r);
                if (t == null || t.getPlant() == null) continue;
                any.add(new int[]{c, r});
                if (t.isWater()) water.add(new int[]{c, r});
            }
        }
        if (!water.isEmpty()) return water.get(RandomUtil.between(0, water.size() - 1));
        if (!any.isEmpty()) return any.get(RandomUtil.between(0, any.size() - 1));
        return new int[]{ Math.max(1, cols / 2), RandomUtil.between(1, rows) };
    }

    // ════════════════════════════════════════════════════════════
    //  ATTACK RESOLUTION
    // ════════════════════════════════════════════════════════════

    private void updateAttacks(Boss boss, GameSession session) {
        Iterator<Boss.BossAttack> it = boss.getAttacks().iterator();
        while (it.hasNext()) {
            Boss.BossAttack atk = it.next();
            atk.timer--;
            if (atk.phase == Boss.BossAttack.Phase.INCOMING) {
                atk.progress = 1f - Math.max(0f, (float) atk.timer / ATTACK_INCOMING_TICKS);
                if (atk.timer <= 0) {
                    atk.phase = Boss.BossAttack.Phase.IMPACT;
                    atk.timer = ATTACK_IMPACT_TICKS;
                    applyAbilityImpact(atk, session);
                }
            } else if (atk.phase == Boss.BossAttack.Phase.IMPACT) {
                if (atk.timer <= 0) atk.phase = Boss.BossAttack.Phase.DONE;
            }
            if (atk.phase == Boss.BossAttack.Phase.DONE) it.remove();
        }
    }

    private void applyAbilityImpact(Boss.BossAttack atk, GameSession session) {
        if (atk.damageApplied) return;
        atk.damageApplied = true;
        switch (atk.type) {
            case EGYPT_MISSILE:
                destroyPlantAt(session, (int) Math.round(atk.targetCol), atk.targetRow);
                spawnTombstones(session, 2);
                break;
            case ICE_MISSILE:
            case DARK_FIREBALL:
                destroyPlantAt(session, (int) Math.round(atk.targetCol), atk.targetRow);
                break;
            case BEACH_SHARK:
                destroyPlantAt(session, (int) Math.round(atk.targetCol), atk.targetRow);
                spawnSharkMinion(session, atk.targetRow);
                break;
            case EGYPT_CHARGE:
            case BEACH_TURBINE:
                destroyPlantsInRow(session, atk.targetRow);
                destroyPlantsInRow(session, atk.targetRow2);
                break;
            case ICE_WIND:
                freezePlantsInRow(session, atk.targetRow);
                freezePlantsInRow(session, atk.targetRow2);
                break;
            case ICE_FREEZE_COLUMN:
                freezePlantsInColumn(session, (int) Math.round(atk.targetCol));
                spawnFrozenZombie(session, (int) Math.round(atk.targetCol));
                break;
            default:
                break;
        }
    }

    // ── اثرگذاری بر گیاهان/کاشی‌ها ─────────────────────────────────────────────────

    private void destroyPlantAt(GameSession session, int col, int row) {
        Tile t = session.getGameMap().getTile(col, row);
        if (t == null || t.getPlant() == null) return;
        t.getPlant().setCurrentHealth(0);
        view.printRaw("[31m💥 Zomboss destroyed the plant at (" + col + "," + row + ")![0m");
    }

    private void destroyPlantsInRow(GameSession session, int row) {
        if (row < 1) return;
        int cols = session.getGameMap().getCols();
        for (int c = 1; c <= cols; c++) {
            Tile t = session.getGameMap().getTile(c, row);
            if (t == null) continue;
            if (t.getPlant() != null) t.getPlant().setCurrentHealth(0);
            if (t.getSecondLayerPlant() != null) t.getSecondLayerPlant().setCurrentHealth(0);
        }
    }

    private void freezePlantsInRow(GameSession session, int row) {
        if (row < 1) return;
        int cols = session.getGameMap().getCols();
        for (int c = 1; c <= cols; c++) {
            Tile t = session.getGameMap().getTile(c, row);
            if (t != null && t.getPlant() != null && !t.getPlant().isFirePlant())
                t.getPlant().incrementFreezeLevel();
        }
    }

    private void freezePlantsInColumn(GameSession session, int col) {
        int rows = session.getGameMap().getRows();
        for (int r = 1; r <= rows; r++) {
            Tile t = session.getGameMap().getTile(col, r);
            if (t != null && t.getPlant() != null && !t.getPlant().isFirePlant())
                t.getPlant().incrementFreezeLevel();
        }
    }

    private void spawnTombstones(GameSession session, int count) {
        int rows = session.getGameMap().getRows();
        int cols = session.getGameMap().getCols();
        int placed = 0, attempts = 0;
        while (placed < count && attempts < 30) {
            attempts++;
            int c = RandomUtil.between(3, cols);
            int r = RandomUtil.between(1, rows);
            Tile t = session.getGameMap().getTile(c, r);
            if (t != null && t.getPlant() == null && t.isPlantable() && !t.isTombstone()) {
                session.getGameMap().setTileType(c, r, TileType.TOMBSTONE);
                placed++;
            }
        }
    }

    // ════════════════════════════════════════════════════════════
    //  ZOMBIE SPAWNS
    // ════════════════════════════════════════════════════════════

    private void spawnMinion(GameSession session, ChapterType chapter) {
        ZombieType[] allowed = ZombieFactory.getAllowedZombiesForChapter(chapter);
        if (allowed == null || allowed.length == 0) return;
        Zombie z = ZombieFactory.create(allowed[RandomUtil.between(0, allowed.length - 1)]);
        if (z == null) return;
        int rows = session.getGameMap().getRows();
        int lane = RandomUtil.between(1, rows);
        z.setX(session.getGameMap().getCols() + 1.0);
        z.setY(lane);
        z.setLane(lane);
        session.getActiveZombies().add(z);
    }

    /** بچه‌کوسه: یک زامبیِ عادی که از سمتِ آب می‌آید. */
    private void spawnSharkMinion(GameSession session, int row) {
        Zombie z = ZombieFactory.create(ZombieType.NORMAL);
        if (z == null) return;
        int rows = session.getGameMap().getRows();
        int lane = row >= 1 && row <= rows ? row : RandomUtil.between(1, rows);
        z.setX(session.getGameMap().getCols() + 1.0);
        z.setY(lane);
        z.setLane(lane);
        session.getActiveZombies().add(z);
    }

    /** زامبیِ یخ‌زده در ستونِ هدف (یخ‌زدنِ ستون). */
    private void spawnFrozenZombie(GameSession session, int col) {
        Zombie z = ZombieFactory.create(ZombieType.NORMAL);
        if (z == null) return;
        int rows = session.getGameMap().getRows();
        int lane = RandomUtil.between(1, rows);
        z.setX(Math.max(1, col));
        z.setY(lane);
        z.setLane(lane);
        z.addEffect(ZombieEffect.FROZEN, 120);
        session.getActiveZombies().add(z);
    }
}
