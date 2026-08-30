package com.pvz2.service;

import com.pvz2.model.Boss;
import com.pvz2.model.GameSession;
import com.pvz2.model.Level;
import com.pvz2.model.Projectile;
import com.pvz2.model.Wave;
import com.pvz2.model.enums.*;
import com.pvz2.model.plants.GenericPlant;
import com.pvz2.model.plants.Plant;
import com.pvz2.model.tiles.Tile;
import com.pvz2.model.zombies.Gargantuar;
import com.pvz2.model.zombies.JesterZombie;
import com.pvz2.model.zombies.Zombie;
import com.pvz2.util.RandomUtil;
import com.pvz2.view.ConsoleView;

import java.util.ArrayList;
import java.util.List;

/**
 * سرویس مبارزه — هر تیک پردازش می‌شود.
 * شامل بررسی کامل شرایط باخت/برد همه ۸ نوع مرحله ویژه.
 */
public class CombatService {

    private final ConsoleView view;
    private com.pvz2.service.LevelProgressService levelProgressService;
    private com.pvz2.service.ScoredGameService scoredGameService;
    private com.pvz2.service.QuestService questService;

    public CombatService(ConsoleView view) {
        this.view = view;
    }

    public void setLevelProgressService(com.pvz2.service.LevelProgressService lps) {
        this.levelProgressService = lps;
    }

    public void setScoredGameService(com.pvz2.service.ScoredGameService sgs) {
        this.scoredGameService = sgs;
    }

    public void setQuestService(com.pvz2.service.QuestService qs) {
        this.questService = qs;
    }

    // ════════════════════════════════════════════════════════
    //  MAIN TICK
    // ════════════════════════════════════════════════════════

    public void processTick(GameSession session) {
        if (!session.isInProgress()) return;
        tickAllPlants(session);
        collectPlantProjectiles(session);
        moveProjectiles(session);
        checkProjectileHits(session);
        moveZombies(session);
        zombiesAttackPlants(session);
        checkZombiesAtEnd(session);
        removeDeadEntities(session);
        applyIceMeltNearFire(session);
        checkTimedWarCondition(session);
    }

    // ════════════════════════════════════════════════════════
    //  PLANTS
    // ════════════════════════════════════════════════════════

    private void tickAllPlants(GameSession session) {
        for (int r = 1; r <= session.getGameMap().getRows(); r++) {
            for (int c = 1; c <= session.getGameMap().getCols(); c++) {
                Tile tile = session.getGameMap().getTile(c, r);
                if (tile == null) continue;
                tickPlantOnTile(tile, session);
            }
        }
    }

    private void tickPlantOnTile(Tile tile, GameSession session) {
        Plant plant = tile.getPlant();
        if (plant != null && plant.isAlive())
            plant.onTick(session.getCurrentTick(), session);
        Plant second = tile.getSecondLayerPlant();
        if (second != null && second.isAlive())
            second.onTick(session.getCurrentTick(), session);
    }

    // ════════════════════════════════════════════════════════
    //  PROJECTILES
    // ════════════════════════════════════════════════════════

    private void collectPlantProjectiles(GameSession session) {
        for (int r = 1; r <= session.getGameMap().getRows(); r++) {
            for (int c = 1; c <= session.getGameMap().getCols(); c++) {
                Tile tile = session.getGameMap().getTile(c, r);
                if (tile == null || tile.getPlant() == null) continue;
                collectFromPlant(tile.getPlant(), session);
            }
        }
    }

    private void collectFromPlant(Plant plant, GameSession session) {
        if (!(plant instanceof GenericPlant)) return;
        GenericPlant gp = (GenericPlant) plant;
        int maxRows = session.getGameMap().getRows();
        for (Projectile proj : gp.pollPendingProjectiles()) {
            proj.setX(plant.getX());
            int targetRow = plant.getY() + proj.getYOffset();
            if (targetRow < 1 || targetRow > maxRows) continue;
            proj.setY(targetRow);
            applyTorchwoodConversion(proj, session);
            session.getActiveProjectiles().add(proj);
        }
    }

    private void applyTorchwoodConversion(Projectile proj, GameSession session) {
        if (proj.getType() != ProjectileType.NORMAL
                && proj.getType() != ProjectileType.ICE) return;
        for (int c = (int) proj.getX() + 1;
             c <= session.getGameMap().getCols(); c++) {
            Tile t = session.getGameMap().getTile(c, proj.getY());
            if (t == null || t.getPlant() == null) continue;
            if (t.getPlant().getType() == PlantType.TORCHWOOD) {
                proj = new Projectile(ProjectileType.FIRE,
                        proj.getX(), proj.getY(), proj.getDamage() * 2);
                break;
            }
        }
    }

    private void moveProjectiles(GameSession session) {
        for (Projectile proj : session.getActiveProjectiles()) {
            if (proj.isMovingRight())
                proj.setX(proj.getX() + proj.getSpeed() * 0.1);
            else
                proj.setX(proj.getX() - proj.getSpeed() * 0.1);
        }
    }

    private void checkProjectileHits(GameSession session) {
        List<Projectile> toRemove = new ArrayList<>();
        // روی یک کپیِ لحظه‌ای پیمایش می‌کنیم چون برخی برخوردها حین همین حلقه پرتابه
        // اضافه می‌کنند (مثلِ منعکس‌کردنِ پرتابه توسطِ زامبیِ Jester در Dark Ages) و
        // بدونِ کپی یک ConcurrentModificationException می‌دهد. پرتابه‌ی تازه، تیکِ بعد
        // پردازش می‌شود.
        for (Projectile proj : new ArrayList<>(session.getActiveProjectiles())) {
            // پرتابه‌ای که به لِینِ رئیس رسیده، به او آسیب می‌زند (مکانیکِ بردِ boss).
            if (hitBoss(proj, session)) {
                toRemove.add(proj);
                continue;
            }
            if (isOutOfBounds(proj, session)) {
                toRemove.add(proj);
                continue;
            }
            if (proj.getTargetX() > 0 && proj.getX() >= proj.getTargetX()) {
                toRemove.add(proj);
                continue;
            }
            if (isTombstoneBlocking(proj, session)) {
                toRemove.add(proj);
                continue;
            }
            if (proj.isHitsPlants()) {
                if (hitPlants(proj, session)) toRemove.add(proj);
                continue;
            }
            boolean hit = checkHitZombies(proj, session);
            if (hit && !proj.isPassesThrough()) toRemove.add(proj);
        }
        session.getActiveProjectiles().removeAll(toRemove);
    }

    private boolean isOutOfBounds(Projectile proj, GameSession session) {
        double x = proj.getX();
        return x < 0 || x > session.getGameMap().getCols() + 1;
    }

    /**
     * برخوردِ پرتابه با رئیس (Zomboss): اگر پرتابه در لِینِ فعلیِ رئیس باشد و به
     * ستونِ او رسیده باشد، به رئیس آسیب می‌زند. این تنها راهِ آسیب‌زدن به رئیس است
     * (بازیکن باید شوتر را در لِینِ رئیس بچیند). پرتابه‌های هدف‌گیرِ گیاه (منفی)
     * روی رئیس اثر ندارند.
     */
    private boolean hitBoss(Projectile proj, GameSession session) {
        Boss boss = session.getBoss();
        if (boss == null || !boss.isVulnerable()) return false;
        if (proj.isHitsPlants()) return false;
        if (!proj.isMovingRight()) return false;
        // رئیس چند سطر را اشغال می‌کند — پرتابه در هر یک از سطرهای اشغالی به او می‌خورد.
        int rows = session.getGameMap().getRows();
        if (!boss.coversRow(proj.getY(), rows)) return false;
        if (proj.getX() >= boss.getX() - 0.6) {
            boss.takeDamage(proj.getDamage());
            return true;
        }
        return false;
    }

    private boolean isTombstoneBlocking(Projectile proj, GameSession session) {
        if (proj.isArc()) return false;
        int col = (int) proj.getX();
        int row = proj.getY();
        Tile tile = session.getGameMap().getTile(col, row);
        if (tile != null && tile.isTombstone()) {
            Tile.Reward reward = tile.getReward();
            tile.takeDamage(proj.getDamage());
            // با اتمامِ جانِ سنگ‌قبر (تبدیل به خانه‌ی معمولی)، جایزه‌اش آزاد می‌شود.
            if (!tile.isTombstone() && reward != Tile.Reward.NONE) {
                releaseGravestoneReward(tile, reward, session);
                tile.setReward(Tile.Reward.NONE);
            }
            return true;
        }
        return false;
    }

    /** آزادسازیِ خورشید یا غذای گیاهِ نهفته در سنگ‌قبرِ نابودشده + توستِ جمع‌آوری. */
    private void releaseGravestoneReward(Tile tile, Tile.Reward reward,
                                         GameSession session) {
        if (reward == Tile.Reward.SUN) {
            com.pvz2.model.Sun sun = new com.pvz2.model.Sun(
                    com.pvz2.model.enums.SunType.NORMAL,
                    tile.getX(), tile.getY(), session.getCurrentTick());
            sun.setLanded(true);
            sun.setFallProgress(1.0);
            session.getActiveSuns().add(sun);
            session.pushCollectEvent("☀ Sun from gravestone!");
        } else if (reward == Tile.Reward.PLANT_FOOD) {
            if (session.addPlantFood())
                session.pushCollectEvent("🌱 Plant Food from gravestone!");
        }
    }

    private boolean hitPlants(Projectile proj, GameSession session) {
        int col = (int) proj.getX();
        Tile tile = session.getGameMap().getTile(col, proj.getY());
        if (tile == null || tile.getPlant() == null) return false;
        Plant plant = tile.getPlant();
        if (proj.getType() == ProjectileType.ICE) plant.incrementFreezeLevel();
        // تیرِ آتشین یک بلاکِ یخِ گیاه را ذوب می‌کند (به‌جایِ آسیب) تا آزاد شود.
        else if (proj.getType() == ProjectileType.FIRE && plant.isFrozen()) plant.meltIceBlock();
        else plant.takeDamage(proj.getDamage());
        return true;
    }

    private boolean checkHitZombies(Projectile proj, GameSession session) {
        boolean hitSomething = false;
        for (Zombie zombie : session.getActiveZombies()) {
            if (!zombie.isAlive()) continue;
            if (Math.abs(zombie.getX() - proj.getX()) < 0.6
                    && zombie.getY() == proj.getY()) {
                // غواصِ زیرِ آب فقط با پرتابه‌ی قوسی (lobber) آسیب می‌بیند؛ بقیه رد می‌شوند.
                if (isSubmergedSnorkel(zombie, session) && !isLobber(proj)) continue;
                // چتردار: تمامِ پرتابه‌های قوسی/لابر را با چترش دفع می‌کند — بی‌اثر و مصرف می‌شوند.
                if (zombie.getType() == ZombieType.PARASOL_ZOMBIE && isLobber(proj)) {
                    return true;
                }
                if (handleJesterDeflect(zombie, proj, session)) return true;
                applyProjectileToZombie(proj, zombie, session);
                updateWaveHealth(session, proj.getDamage());
                hitSomething = true;
            }
        }
        return hitSomething;
    }

    /** غواص وقتی روی خانه‌ی آب و در حالِ خوردنِ گیاه نباشد، زیرِ آب (مصون) است. */
    private boolean isSubmergedSnorkel(Zombie z, GameSession session) {
        if (z.getType() != ZombieType.SNORKEL_ZOMBIE || z.isAttacking()) return false;
        int col = (int) Math.round(z.getX());
        if (!session.getGameMap().isValidPosition(col, z.getY())) return false;
        Tile t = session.getGameMap().getTile(col, z.getY());
        return t != null && t.isWater();
    }

    private boolean isLobber(Projectile proj) {
        return proj.isArc() || proj.getType() == ProjectileType.LOBBED;
    }

    private boolean anyOtherWizardAlive(GameSession session, Zombie dying) {
        for (Zombie z : session.getActiveZombies()) {
            if (z != dying && z.isAlive() && z.getType() == ZombieType.WIZARD_ZOMBIE) return true;
        }
        return false;
    }

    private void revertAllSheep(GameSession session) {
        for (int r = 1; r <= session.getGameMap().getRows(); r++) {
            for (int c = 1; c <= session.getGameMap().getCols(); c++) {
                Tile t = session.getGameMap().getTile(c, r);
                if (t == null) continue;
                if (t.getPlant() != null)
                    t.getPlant().removeEffect(com.pvz2.model.enums.PlantEffect.WIZARDED);
                if (t.getSecondLayerPlant() != null)
                    t.getSecondLayerPlant().removeEffect(com.pvz2.model.enums.PlantEffect.WIZARDED);
            }
        }
    }

    private boolean handleJesterDeflect(Zombie zombie, Projectile proj,
                                        GameSession session) {
        if (!(zombie instanceof JesterZombie)) return false;
        JesterZombie jester = (JesterZombie) zombie;
        Projectile deflected = jester.deflect(proj);
        session.getActiveProjectiles().add(deflected);
        return true;
    }

    private void applyProjectileToZombie(Projectile proj, Zombie zombie, GameSession session) {
        // اژدها-ایمپ به آتش مصون است — تیرِ آتشین هیچ اثری روی او ندارد.
        if (zombie.getType() == ZombieType.DRAGON_IMP
                && proj.getType() == ProjectileType.FIRE) return;
        int dmg = proj.getDamage();
        // فصلِ غارهای یخی: زامبی‌ها با تیرِ یخی سرد/یخ‌زده نمی‌شوند (ایمنی به یخ).
        boolean iceImmune = session.getGameMap().getChapter()
                == com.pvz2.model.enums.ChapterType.FROSTBITE_CAVES;
        if (proj.getType() == ProjectileType.FIRE) {
            dmg *= 2;
            zombie.removeEffect(ZombieEffect.CHILLED);
            // اگر زامبی داخلِ بلاکِ یخ است، تیرِ آتشین یک مرحله آن را ذوب می‌کند.
            zombie.meltIceBlock();
            zombie.addEffect(ZombieEffect.BURNING, 30);
        }
        if (proj.getType() == ProjectileType.POISON)
            zombie.takePoisonDamage(dmg);
        else
            zombie.takeDamage(dmg);
        if (proj.getType() == ProjectileType.ICE && !iceImmune)
            zombie.addEffect(ZombieEffect.CHILLED, 50);
        // مشعلِ Explorer: تیرِ یخی خاموش، تیرِ آتشین دوباره روشنش می‌کند.
        if (zombie.getType() == ZombieType.EXPLORER_ZOMBIE) {
            if (proj.getType() == ProjectileType.ICE)  zombie.setTorchLit(false);
            if (proj.getType() == ProjectileType.FIRE) zombie.setTorchLit(true);
        }
        if (proj.isStunOnHit())
            zombie.addEffect(ZombieEffect.STUNNED, 60);
        if (proj.isExplodes() && proj.getAoeRadius() > 0)
            applyAoEDamage(zombie, proj, dmg);
    }

    private void applyAoEDamage(Zombie center, Projectile proj, int baseDmg) {
        // handled at higher level
    }

    private void updateWaveHealth(GameSession session, int damage) {
        Wave wave = session.getCurrentWave();
        if (wave != null) wave.registerHealthLost(damage);
    }

    // ════════════════════════════════════════════════════════
    //  ZOMBIE MOVEMENT
    // ════════════════════════════════════════════════════════

    private void moveZombies(GameSession session) {
        for (Zombie zombie : session.getActiveZombies()) {
            if (!zombie.isAlive() || zombie.isAttacking()) continue;
            if (isBlockedByPlant(zombie, session)) {
                zombie.setAttacking(true);
                continue;
            }
            zombie.move();
            applySlipperyTile(zombie, session);
        }
    }

    private boolean isBlockedByPlant(Zombie zombie, GameSession session) {
        int col = (int) Math.ceil(zombie.getX());
        if (!session.getGameMap().isValidPosition(col, zombie.getY())) return false;
        Tile tile = session.getGameMap().getTile(col, zombie.getY());
        if (tile == null || tile.getPlant() == null) return false;
        // گیاهِ گوسفندشده (Wizard) خورده نمی‌شود — زامبی از رویش رد می‌شود.
        if (tile.getPlant().isSheep()) return false;
        // Dodo Rider از موانع (گردو/گیاهانِ پرجان/مین) پرواز می‌کند؛ فقط Tall-nut و
        // گیاهانِ عادی جلویش را می‌گیرند.
        if (zombie.getType() == ZombieType.DODO_RIDER && dodoFliesOver(tile.getPlant())) {
            return false;
        }
        return true;
    }

    /** آیا Dodo Rider از روی این گیاه پرواز می‌کند؟ (نه از روی Tall-nut). */
    private boolean dodoFliesOver(Plant plant) {
        PlantType t = plant.getType();
        if (t == PlantType.TALL_NUT) return false;   // بلندتر از آن که رد شود
        if (t == PlantType.WALL_NUT || t == PlantType.PUMPKIN || t == PlantType.ENDURIAN
                || t == PlantType.EXPLODE_O_NUT || t == PlantType.POTATO_MINE
                || t == PlantType.PRIMAL_POTATO_MINE) {
            return true;
        }
        return plant.getMaxHealth() >= 1000;         // سایرِ گیاهانِ پرجان
    }

    private void applySlipperyTile(Zombie zombie, GameSession session) {
        int col = (int) Math.round(zombie.getX());
        int row = zombie.getY();
        if (!session.getGameMap().isValidPosition(col, row)) return;
        Tile tile = session.getGameMap().getTile(col, row);
        if (tile == null || !tile.isSlippery()) return;
        int newRow = tile.isSlipperyUp() ? row - 1 : row + 1;
        if (session.getGameMap().isValidPosition(1, newRow)) {
            zombie.setY(newRow);
            zombie.setLane(newRow);
        }
    }

    // ════════════════════════════════════════════════════════
    //  ZOMBIE ATTACKS
    // ════════════════════════════════════════════════════════

    private void zombiesAttackPlants(GameSession session) {
        for (Zombie zombie : session.getActiveZombies()) {
            if (!zombie.isAlive() || !zombie.isAttacking()) continue;
            int col = (int) Math.ceil(zombie.getX());
            Tile tile = session.getGameMap().getTile(col, zombie.getY());
            if (tile == null || tile.getPlant() == null) {
                zombie.setAttacking(false);
                continue;
            }
            Plant plant = tile.getPlant();
            boolean handled = plant.onZombieAttack(zombie, session);
            if (!handled) {
                int dmgPerTick = zombie.getDamagePerSecond() / 10;
                plant.takeDamage(dmgPerTick);
            }
            if (!plant.isAlive()) {
                handlePlantDestroyed(tile, plant, zombie, session, col,
                        zombie.getY());
            }
        }
    }

    private void handlePlantDestroyed(Tile tile, Plant plant,
                                      Zombie zombie, GameSession session,
                                      int x, int y) {
        plant.onPlantDestroyed(zombie, session);
        view.printPlantDestroyed(plant.getType().name(), x, y);

        if (tile.getSecondLayerPlant() != null) {
            tile.setPlant(tile.getSecondLayerPlant());
            tile.setSecondLayerPlant(null);
        } else {
            tile.setPlant(null);
        }
        // Beghouled: خانه‌ی گیاهِ خورده‌شده به crater تبدیل می‌شود (دیگر گیاه نمی‌گیرد).
        if (session.getLevel() != null
                && session.getLevel().getLevelType() == LevelType.BEGHOULED) {
            tile.setCrater(true);
        }
        zombie.setAttacking(false);
        session.setPlantsLost(session.getPlantsLost() + 1);

        // ── بررسی شرایط باخت مراحل ویژه ──
        checkSpecialLevelLoseOnPlantDestroyed(session, plant, x, y);
    }

    // ════════════════════════════════════════════════════════
    //  SPECIAL LEVEL — LOSE CONDITIONS
    // ════════════════════════════════════════════════════════

    /**
     * بررسی شرایط باخت بعد از نابودی هر گیاه.
     * شامل: LOVE_YOUR_PLANTS، SAVE_OUR_SEEDS
     */
    private void checkSpecialLevelLoseOnPlantDestroyed(GameSession session,
                                                        Plant plant, int x, int y) {
        Level level = session.getLevel();
        if (level == null || !session.isInProgress()) return;

        switch (level.getLevelType()) {

            // ── LOVE_YOUR_PLANTS: تعداد مجاز گیاه از دست رفته ─────
            case LOVE_YOUR_PLANTS:
                if (session.getPlantsLost() >= level.getMaxPlantsLost()) {
                    session.setResult(GameResult.LOSE);
                    view.printRaw("\u001B[31m\u001B[1m"
                            + "💔 You've lost too many plants! (" + session.getPlantsLost()
                            + "/" + level.getMaxPlantsLost() + ") — GAME OVER!\u001B[0m");
                    view.printGameOver();
                }
                break;

            // ── SAVE_OUR_SEEDS: گیاه محافظت‌شده خورده شد ──────────
            case SAVE_OUR_SEEDS:
                if (session.isProtectedPosition(x, y)) {
                    session.removeProtectedPosition(x, y);
                    session.setResult(GameResult.LOSE);
                    view.printRaw("\u001B[31m\u001B[1m"
                            + "🌱 A protected seed was eaten at (" + x + "," + y
                            + ")! — GAME OVER!\u001B[0m");
                    view.printGameOver();
                }
                break;

            default:
                break;
        }
    }

    // ════════════════════════════════════════════════════════
    //  DEAD_LINE CHECK — زامبی از خط ممنوع رد شد
    // ════════════════════════════════════════════════════════

    private void checkZombiesAtEnd(GameSession session) {
        for (Zombie zombie : session.getActiveZombies()) {
            if (!zombie.isAlive()) continue;

            // ── DEAD_LINE: رسیدن به ستون ممنوع ─────────────────────
            if (checkDeadLine(zombie, session)) continue;

            // ── رسیدن به انتهای مسیر (ماشین چمن‌زنی) ───────────────
            if (zombie.getX() <= 0) {
                handleZombieReachedEnd(zombie, session);
            }
        }
    }

    /**
     * بررسی DEAD_LINE: اگر زامبی از ستون deadLineColumn عبور کرد → باخت.
     * @return true اگر بازی تموم شد
     */
    private boolean checkDeadLine(Zombie zombie, GameSession session) {
        Level level = session.getLevel();
        if (level == null || level.getLevelType() != LevelType.DEAD_LINE) {
            return false;
        }
        int deadCol = level.getDeadLineColumn();
        // زامبی‌ها از راست به چپ می‌آیند؛ X<=deadCol یعنی از خط رد شده
        if (zombie.getX() <= deadCol && zombie.isAlive()) {
            session.setResult(GameResult.LOSE);
            view.printRaw("\u001B[31m\u001B[1m"
                    + "🚫 A zombie crossed the DEAD LINE at column "
                    + deadCol + "! — GAME OVER!\u001B[0m");
            view.printGameOver();
            return true;
        }
        return false;
    }

    private void handleZombieReachedEnd(Zombie zombie, GameSession session) {
        // من‌زامبی + VERSUS: زامبیِ رسیده به چپ، مغزِ آن ردیف را می‌خورد.
        if (session.getLevel() != null
                && (session.getLevel().getLevelType() == LevelType.I_ZOMBIE
                    || session.getLevel().getLevelType() == LevelType.VERSUS)) {
            Object ms = session.getMinigameState();
            if (ms instanceof com.pvz2.model.IZombieState) {
                ((com.pvz2.model.IZombieState) ms).eatBrain(zombie.getY());
            }
            zombie.setCurrentHealth(0); // زامبی پس از خوردن مغز محو می‌شود
            return;
        }
        int rowIndex = zombie.getY() - 1;
        if (session.getGameMap().isLawnMowerAvailable(rowIndex)) {
            triggerLawnMower(zombie, rowIndex, session);
        } else {
            session.setResult(GameResult.LOSE);
            view.printGameOver();
        }
    }

    /** سرعتِ حرکتِ چمن‌زن (ستون در هر تیک). */
    private static final double MOWER_SPEED = 0.35;

    private void triggerLawnMower(Zombie trigger, int rowIndex, GameSession session) {
        session.getGameMap().setLawnMowerUsed(rowIndex);
        // چمن‌زنِ متحرک از سرِ لاین به راه می‌افتد؛ بقیه‌ی زامبی‌های لاین را یکی‌یکی
        // (نه همه با هم) در tickMowers می‌کشد. زامبیِ رسیده به خانه فوراً کشته می‌شود.
        session.launchMower(rowIndex + 1);
        if (trigger != null && trigger.isAlive()) {
            trigger.setCurrentHealth(0);
            session.setZombiesKilled(session.getZombiesKilled() + 1);
        }
        view.printLawnMowerTriggered(rowIndex + 1,
                trigger != null ? java.util.Arrays.asList(trigger.getType().name())
                                : new ArrayList<>());
        onLawnmowerKill(1);
    }

    /**
     * چمن‌زن‌های متحرک را هر تیک جلو می‌برد و هر زامبیِ لاین که به آن می‌رسد را
     * می‌کشد؛ با رسیدن به انتهای لاین حذف می‌شود. باید هر تیک صدا زده شود.
     */
    public void tickMowers(GameSession session) {
        java.util.List<com.pvz2.model.GameSession.ActiveMower> mowers =
                session.getActiveMowers();
        if (mowers.isEmpty()) return;
        int cols = session.getGameMap().getCols();
        java.util.Iterator<com.pvz2.model.GameSession.ActiveMower> it = mowers.iterator();
        while (it.hasNext()) {
            com.pvz2.model.GameSession.ActiveMower m = it.next();
            m.x += MOWER_SPEED;
            for (Zombie z : session.getActiveZombies()) {
                if (z.isAlive() && z.getY() == m.row
                        && Math.abs(z.getX() - m.x) < 0.6) {
                    z.setCurrentHealth(0);
                    session.setZombiesKilled(session.getZombiesKilled() + 1);
                    onLawnmowerKill(1);
                }
            }
            if (m.x > cols + 1) it.remove();
        }
    }

    // ════════════════════════════════════════════════════════
    //  TIMED_WAR CHECK — بررسی برد/باخت نبرد زماندار
    // ════════════════════════════════════════════════════════

    /**
     * هر تیک بررسی می‌کند آیا هدف Timed War رسیده یا وقت تموم شده.
     */
    private void checkTimedWarCondition(GameSession session) {
        Level level = session.getLevel();
        if (level == null || level.getLevelType() != LevelType.TIMED_WAR) return;
        if (!session.isInProgress()) return;
        if (session.isTimedWarGoalReached()) return;

        boolean goalMet;
        if (level.isTimedWarSunMode()) {
            // حالت خورشید: جمع‌آوری مقدار هدف خورشید
            goalMet = session.getTimedWarSunAchieved() >= level.getTimedWarSunTarget();
        } else {
            // حالت زامبی: کشتن تعداد هدف زامبی
            goalMet = session.getTimedWarKillsAchieved() >= level.getTimedWarZombieTarget();
        }

        if (goalMet) {
            session.setTimedWarGoalReached(true);
            session.setResult(GameResult.WIN);
            view.printRaw("\u001B[32m\u001B[1m"
                    + "⏱ Timed War objective reached! VICTORY!\u001B[0m");
            view.printGameWon();
            notifyLevelComplete(session);
            return;
        }

        // بررسی اتمام وقت
        if (session.isTimedWarExpired()) {
            session.setResult(GameResult.LOSE);
            String objective = level.isTimedWarSunMode()
                    ? "Collected " + session.getTimedWarSunAchieved()
                    + "/" + level.getTimedWarSunTarget() + " sun"
                    : "Killed " + session.getTimedWarKillsAchieved()
                    + "/" + level.getTimedWarZombieTarget() + " zombies";
            view.printRaw("\u001B[31m\u001B[1m"
                    + "⏱ Time's up! " + objective + " — GAME OVER!\u001B[0m");
            view.printGameOver();
        }
    }

    // ════════════════════════════════════════════════════════
    //  DEAD ENTITIES
    // ════════════════════════════════════════════════════════

    private void removeDeadEntities(GameSession session) {
        for (int r = 1; r <= session.getGameMap().getRows(); r++) {
            for (int c = 1; c <= session.getGameMap().getCols(); c++) {
                Tile tile = session.getGameMap().getTile(c, r);
                if (tile == null) continue;
                Plant plant = tile.getPlant();
                if (plant != null && !plant.isAlive()) {
                    plant.onPlantDestroyed(null, session);
                    if (tile.getSecondLayerPlant() != null) {
                        tile.setPlant(tile.getSecondLayerPlant());
                        tile.setSecondLayerPlant(null);
                    } else {
                        tile.setPlant(null);
                    }
                }
            }
        }
        List<Zombie> toRemove = new ArrayList<>();
        for (Zombie z : session.getActiveZombies()) {
            if (!z.isAlive()) {
                handleZombieDeath(z, session);
                toRemove.add(z);
            }
        }
        // الگوی امتیازی: کشتنِ همزمانِ ۳+ زامبی (انفجار)
        if (toRemove.size() >= 3) {
            session.pushMeoEvent("💥 AoE x" + toRemove.size() + " SIMULTANEOUS!",
                    750L * toRemove.size());
        }
        session.getActiveZombies().removeAll(toRemove);
        session.getActiveProjectiles().removeIf(
                p -> p.getX() < 0 || p.getX() > session.getGameMap().getCols() + 2);
    }

    private void handleZombieDeath(Zombie zombie, GameSession session) {
        view.printZombieDead(zombie.getType().name(), zombie.getX(), zombie.getY());
        session.setZombiesKilled(session.getZombiesKilled() + 1);

        // Wizard: با مرگِ آخرین جادوگر، همه‌ی گیاهانِ گوسفندشده به حالتِ عادی برمی‌گردند.
        if (zombie.getType() == ZombieType.WIZARD_ZOMBIE
                && !anyOtherWizardAlive(session, zombie)) {
            revertAllSheep(session);
        }

        // خورشیددزدها هنگامِ مرگ خورشیدِ نگه‌داشته را برمی‌گردانند:
        //   Ra → همه‌ی خورشید به بازیکن؛ Turquoise → نیمی روی زمین (قابلِ برداشت).
        handleSunStealerDeath(zombie, session);

        // ثبت کشتن زامبی برای TIMED_WAR
        if (session.getLevel() != null
                && session.getLevel().getLevelType() == LevelType.TIMED_WAR
                && !session.getLevel().isTimedWarSunMode()) {
            session.registerTimedWarKill();
        }

        zombie.onDeath(session);
        updateMeoPoints(zombie, session);
        if (questService != null) {
            questService.onZombieKilled(
                    zombie.getType().name(),
                    session.getGameMap().getChapter(), null);
        }
        handleDrops(zombie, session);
        if (zombie.isGlowing()) {
            boolean added = session.addPlantFood();
            if (added) {
                view.printGlowingZombieDroppedFood(session.getPlantFoodCount());
                session.pushCollectEvent("🌱 Plant Food collected!");
            }
        }
        Wave wave = session.getCurrentWave();
        if (wave != null) wave.registerHealthLost(zombie.getMaxHealth());
    }

    /**
     * مرگِ خورشیددزدها. Ra: همه‌ی خورشیدِ نگه‌داشته را مستقیم به بازیکن برمی‌گرداند.
     * Turquoise: نیمی از خورشیدِ دزدیده‌شده را به‌صورتِ خورشیدِ افتاده روی زمین می‌ریزد
     * (بازیکن می‌تواند بردارد).
     */
    private void handleSunStealerDeath(Zombie zombie, GameSession session) {
        if (!(zombie instanceof com.pvz2.model.zombies.NormalZombie)) return;
        // اگر Ra وسطِ کشیدنِ خورشیدها بمیرد، خورشیدهای بنفشِ آن ردیف را به حالتِ
        // عادیِ قابلِ برداشت برگردان (نگذار برای همیشه بنفش/معلق بمانند).
        if (zombie.getType() == ZombieType.RA_ZOMBIE) {
            int row = zombie.getY();
            for (com.pvz2.model.Sun s : session.getActiveSuns()) {
                if (s.isBeingStolen() && s.getStealerRow() == row) {
                    s.setBeingStolen(false);
                    s.setStealProgress(0);
                }
            }
        }
        int stolen = ((com.pvz2.model.zombies.NormalZombie) zombie).getStolenSun();
        if (stolen <= 0) return;
        if (zombie.getType() == ZombieType.RA_ZOMBIE) {
            session.addSun(stolen);
            session.pushCollectEvent("☀ Ra returned " + stolen + " sun!");
        } else if (zombie.getType() == ZombieType.TURQUOISE_ZOMBIE) {
            int half = stolen / 2;
            if (half <= 0) return;
            com.pvz2.model.Sun sun = new com.pvz2.model.Sun(
                    com.pvz2.model.enums.SunType.NORMAL,
                    (int) Math.round(zombie.getX()), zombie.getY(),
                    session.getCurrentTick());
            sun.setLanded(true);
            sun.setFallProgress(1.0);
            sun.setValue(half);
            session.getActiveSuns().add(sun);
            session.pushCollectEvent("☀ Turquoise dropped " + half + " sun!");
        }
    }

    private void updateMeoPoints(Zombie zombie, GameSession session) {
        long now = session.getCurrentTick();
        if (now - session.getLastKillTick() <= 30) {
            session.setConsecutiveKills(session.getConsecutiveKills() + 1);
        } else {
            session.setConsecutiveKills(1);
        }
        session.setLastKillTick(now);
        long points = 10L * session.getConsecutiveKills();
        session.setMeoPoints(session.getMeoPoints() + points);
        // الگوی امتیازی: کمبوی کشتنِ سریع (اعلان یک‌بار در هر رگبار)
        if (session.getConsecutiveKills() == 3) {
            session.pushMeoEvent("⚡ SPEED-KILL COMBO!", 300L * 3);
        }
    }

    private void handleDrops(Zombie zombie, GameSession session) {
        if (!RandomUtil.chance(0.1)) return;
        com.pvz2.model.User user = com.pvz2.model.AppState.getInstance().getCurrentUser();
        if (user == null) return;
        double roll = RandomUtil.nextDouble();
        if (roll < 0.33) {
            user.setCoins(user.getCoins() + 50);
            view.printZombieDropped("coin", (int) user.getCoins());
            session.pushCollectEvent("🪙 +50 Coins collected!");
        } else if (roll < 0.66) {
            user.setGems(user.getGems() + 1);
            view.printZombieDropped("diamond", user.getGems());
            session.pushCollectEvent("💎 +1 Diamond collected!");
        } else {
            user.setPots(user.getPots() + 1);
            view.printZombieDropped("pot", user.getPots());
            session.pushCollectEvent("🏺 +1 Pot collected!");
        }
    }

    private void applyIceMeltNearFire(GameSession session) {
        for (int r = 1; r <= session.getGameMap().getRows(); r++) {
            for (int c = 1; c <= session.getGameMap().getCols(); c++) {
                Tile tile = session.getGameMap().getTile(c, r);
                if (tile == null || tile.getPlant() == null) continue;
                Plant p = tile.getPlant();
                if (p.isFrozen() && hasAdjacentFirePlant(session, c, r)) {
                    p.takeDamage(6);
                    if (p.getFreezeLevel() > 0 && RandomUtil.chance(0.1)) {
                        p.setFreezeLevel(p.getFreezeLevel() - 1);
                        if (p.getFreezeLevel() < 3) p.thaw();
                    }
                }
            }
        }
    }

    private boolean hasAdjacentFirePlant(GameSession session, int cx, int cy) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                Tile t = session.getGameMap().getTile(cx + dx, cy + dy);
                if (t != null && t.getPlant() != null && t.getPlant().isFirePlant())
                    return true;
            }
        }
        return false;
    }

    // ════════════════════════════════════════════════════════
    //  WIN CONDITION
    // ════════════════════════════════════════════════════════

    public void checkWinCondition(GameSession session) {
        if (!session.isInProgress()) return;
        if (checkVasebreakerWin(session)) return;
        if (checkIZombieEnd(session)) return;
        if (checkVersusEnd(session)) return;
        if (checkBeghouledEnd(session)) return;
        if (checkBossDefeated(session)) return;
        if (session.allWavesFinished()) {
            session.setResult(GameResult.WIN);
            view.printGameWon();
            notifyLevelComplete(session);
            if (questService != null) questService.onGameWon(session);
        }
    }

    /**
     * برد کوزه‌شکنی: وقتی همه‌ی کوزه‌ها شکسته و همه‌ی زامبی‌ها نابود شده‌اند.
     * برخلاف مراحل عادی، پیشرویِ فصل ماجراجویی را باز نمی‌کند (مینی‌گیم مستقل است)؛
     * فقط شمارنده‌ی مینی‌گیم‌های کاربر افزایش می‌یابد.
     */
    private boolean checkVasebreakerWin(GameSession session) {
        Level level = session.getLevel();
        if (level == null || level.getLevelType() != LevelType.VASEBREAKER) return false;
        Object ms = session.getMinigameState();
        if (!(ms instanceof com.pvz2.model.VasebreakerState)) return false;
        com.pvz2.model.VasebreakerState vb = (com.pvz2.model.VasebreakerState) ms;
        if (!vb.allBroken()) return false;
        for (Zombie z : session.getActiveZombies()) {
            if (z.isAlive()) return false;
        }
        session.setResult(GameResult.WIN);
        view.printGameWon();
        com.pvz2.model.User user = com.pvz2.model.AppState.getInstance().getCurrentUser();
        if (user != null) {
            user.setMinigamesCompleted(user.getMinigamesCompleted() + 1);
        }
        return true;
    }

    /**
     * برد/باختِ من‌زامبی. برد: خوردنِ همه‌ی مغزها. باخت: نبودِ زامبیِ زنده و
     * نبودِ خورشیدِ کافی برای کاشتِ ارزان‌ترین زامبی. مثل کوزه‌شکنی، پیشرویِ
     * فصلِ ماجراجویی را باز نمی‌کند.
     */
    private boolean checkIZombieEnd(GameSession session) {
        Level level = session.getLevel();
        if (level == null || level.getLevelType() != LevelType.I_ZOMBIE) return false;
        Object ms = session.getMinigameState();
        if (!(ms instanceof com.pvz2.model.IZombieState)) return false;
        com.pvz2.model.IZombieState st = (com.pvz2.model.IZombieState) ms;
        if (st.allBrainsEaten()) {
            session.setResult(GameResult.WIN);
            view.printGameWon();
            com.pvz2.model.User user = com.pvz2.model.AppState.getInstance().getCurrentUser();
            if (user != null) user.setMinigamesCompleted(user.getMinigamesCompleted() + 1);
            return true;
        }
        boolean anyZombie = false;
        for (Zombie z : session.getActiveZombies()) {
            if (z.isAlive()) { anyZombie = true; break; }
        }
        if (!anyZombie && session.getSunAmount() < st.minCost()) {
            session.setResult(GameResult.LOSE);
            view.printGameOver();
            return true;
        }
        return false;
    }

    /**
     * برد/باختِ Beghouled. برد: ساختِ هدفِ تعیین‌شده ترکیب → تمام زامبی‌ها نابود.
     * باخت: از قواعدِ عادی می‌آید (زامبی به خانه برسد). همیشه true برمی‌گرداند تا
     * بردِ موج‌محور (allWavesFinished روی موجِ خالی) فعال نشود.
     */
    /**
     * برد/باختِ VERSUS (دونفره): اگر همه‌ی مغزها خورده شوند بازیکنِ زامبی می‌برد؛
     * اگر تایمرِ ۲ دقیقه تمام شود بازیکنِ گیاه می‌برد. برنده در
     * {@link GameSession#getVersusWinnerRole()} ("PLANT"/"ZOMBIE") ثبت می‌شود تا
     * لایه‌ی شبکه نتیجه را به سرور گزارش دهد. همیشه در حالتِ پایان true برمی‌گرداند
     * تا بردِ موج‌محور فعال نشود.
     */
    private boolean checkVersusEnd(GameSession session) {
        Level level = session.getLevel();
        if (level == null || level.getLevelType() != LevelType.VERSUS) return false;
        if (session.getVersusWinnerRole() != null) return true; // قبلاً تمام شده
        Object ms = session.getMinigameState();
        if (ms instanceof com.pvz2.model.IZombieState
                && ((com.pvz2.model.IZombieState) ms).allBrainsEaten()) {
            session.setVersusWinnerRole("ZOMBIE");
            session.setResult(GameResult.LOSE); // از دیدِ میزبان (گیاه‌کار)
            return true;
        }
        if (session.getVersusTicksLeft() <= 0) {
            session.setVersusWinnerRole("PLANT");
            session.setResult(GameResult.WIN);  // از دیدِ میزبان (گیاه‌کار)
            return true;
        }
        return false;
    }

    private boolean checkBeghouledEnd(GameSession session) {
        Level level = session.getLevel();
        if (level == null || level.getLevelType() != LevelType.BEGHOULED) return false;
        Object ms = session.getMinigameState();
        if (!(ms instanceof com.pvz2.model.BeghouledState)) return true;
        com.pvz2.model.BeghouledState st = (com.pvz2.model.BeghouledState) ms;
        if (st.isComplete()) {
            // برد: تمام زامبی‌های موجود در باغ از بین می‌روند (طبق داک).
            for (Zombie z : session.getActiveZombies()) z.setCurrentHealth(0);
            session.setResult(GameResult.WIN);
            view.printGameWon();
            com.pvz2.model.User user = com.pvz2.model.AppState.getInstance().getCurrentUser();
            if (user != null) user.setMinigamesCompleted(user.getMinigamesCompleted() + 1);
        }
        return true;   // بردِ موج‌محور را مسدود کن (Beghouled موجِ استاندارد ندارد)
    }

    /**
     * بردِ مرحله‌ی رئیس: وقتی هر ۳ بخشِ سلامتیِ Zomboss تخلیه شد و انیمیشنِ مرگش
     * تمام شد ({@link Boss#isDefeated()}). مثلِ مراحلِ عادی، پیشرویِ فصلِ ماجراجویی
     * را باز می‌کند (رئیس مرحله‌ی ۴ی هر فصل است → {@link #notifyLevelComplete}).
     */
    private boolean checkBossDefeated(GameSession session) {
        Boss boss = session.getBoss();
        if (boss == null) return false;
        if (boss.isDefeated()) {
            session.setResult(GameResult.WIN);
            view.printGameWon();
            notifyLevelComplete(session);
            if (questService != null) questService.onGameWon(session);
            return true;
        }
        return false;
    }

    private void notifyLevelComplete(GameSession session) {
        com.pvz2.model.User user = com.pvz2.model.AppState.getInstance().getCurrentUser();
        if (user == null || session.getLevel() == null) return;
        com.pvz2.model.enums.ChapterType chapter = session.getLevel().getChapter();
        int levelNum = session.getLevelNumber();
        if (levelProgressService != null) {
            levelProgressService.onLevelCompleted(user, chapter, levelNum);
        }
        if (scoredGameService != null) {
            scoredGameService.saveHighScore(user, session.getMeoPoints());
        }
    }

    // ════════════════════════════════════════════════════════
    //  PUBLIC HOOKS
    // ════════════════════════════════════════════════════════

    public void onSunCollectedForQuest(int amount) {
        if (questService != null) questService.onSunCollected(amount);
    }

    public void onExplosiveUsed() {
        if (questService != null) questService.onExplosiveUsed();
    }

    public void onLawnmowerKill(int count) {
        if (questService != null) questService.onLawnmowerKill(count);
    }
}
