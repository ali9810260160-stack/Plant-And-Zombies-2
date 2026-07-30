package service;

import model.GameSession;
import model.Projectile;
import model.Wave;
import model.enums.*;
import model.plants.GenericPlant;
import model.plants.Plant;
import model.tiles.Tile;
import model.zombies.Gargantuar;
import model.zombies.JesterZombie;
import model.zombies.Zombie;
import util.RandomUtil;
import view.ConsoleView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * سرویس مرکزی مبارزه — هر تیک پردازش می‌شود.
 * شامل پیاده‌سازی کامل توانایی‌های منحصربه‌فرد گیاهان.
 */
public class CombatService {

    private final ConsoleView view;
    private service.LevelProgressService levelProgressService;
    private service.ScoredGameService scoredGameService;
    private service.QuestService questService;

    public CombatService(ConsoleView view) {
        this.view = view;
    }

    public void setLevelProgressService(service.LevelProgressService lps) {
        this.levelProgressService = lps;
    }

    public void setScoredGameService(service.ScoredGameService sgs) {
        this.scoredGameService = sgs;
    }

    public void setQuestService(service.QuestService qs) {
        this.questService = qs;
    }

    public void processTick(GameSession session) {
        if (!session.isInProgress()) {
            return;
        }
        tickAllPlants(session);
        collectPlantProjectiles(session);
        moveProjectiles(session);
        checkProjectileHits(session);
        moveZombies(session);
        zombiesAttackPlants(session);
        checkZombiesAtEnd(session);
        removeDeadEntities(session);
        applyIceMeltNearFire(session);
    }

    private void tickAllPlants(GameSession session) {
        for (int r = 1; r <= session.getGameMap().getRows(); r++) {
            for (int c = 1; c <= session.getGameMap().getCols(); c++) {
                Tile tile = session.getGameMap().getTile(c, r);
                if (tile == null) { continue; }
                tickPlantOnTile(tile, session);
            }
        }
    }

    private void tickPlantOnTile(Tile tile, GameSession session) {
        Plant plant = tile.getPlant();
        if (plant != null && plant.isAlive())
            plant.onTick(session.getCurrentTick(), session);

        Plant secondPlant = tile.getSecondLayerPlant();
        if (secondPlant != null && secondPlant.isAlive())
            secondPlant.onTick(session.getCurrentTick(), session);
    }

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
            Plant p = t.getPlant();
            if (p.getType() == PlantType.TORCHWOOD) {
                proj = new Projectile(ProjectileType.FIRE,
                        proj.getX(), proj.getY(),
                        proj.getDamage() * 2);
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
        for (Projectile proj : session.getActiveProjectiles()) {
            if (isOutOfBounds(proj, session)) {
                toRemove.add(proj);
                continue;
            }
            // پرتابه‌های کوتاه‌برد (Sea-shroom / Puff-shroom)
            if (proj.getTargetX() > 0 && proj.getX() >= proj.getTargetX()) {
                toRemove.add(proj); continue;
            }
            if (isTombstoneBlocking(proj, session)) {
                toRemove.add(proj);
                continue;
            }
            if (proj.isHitsPlants()) {
                if (hitPlants(proj, session)) {
                    toRemove.add(proj);
                }
                continue;
            }
            boolean hit = checkHitZombies(proj, session);
            if (hit && !proj.isPassesThrough()) {
                toRemove.add(proj);
            }
        }
        session.getActiveProjectiles().removeAll(toRemove);
    }

    private boolean isOutOfBounds(Projectile proj, GameSession session) {
        double x = proj.getX();
        return x < 0 || x > session.getGameMap().getCols() + 1;
    }

    private boolean isTombstoneBlocking(Projectile proj, GameSession session) {
        if (proj.isArc()) return false;
        int col = (int) proj.getX();
        int row = proj.getY();
        Tile tile = session.getGameMap().getTile(col, row);
        if (tile != null && tile.isTombstone()) {
            tile.takeDamage(proj.getDamage());
            return true;
        }
        return false;
    }

    private boolean hitPlants(Projectile proj, GameSession session) {
        int col = (int) proj.getX();
        Tile tile = session.getGameMap().getTile(col, proj.getY());
        if (tile == null || tile.getPlant() == null) return false;
        Plant plant = tile.getPlant();
        if (proj.getType() == ProjectileType.ICE) plant.incrementFreezeLevel();
        else plant.takeDamage(proj.getDamage());
        return true;
    }

    private boolean checkHitZombies(Projectile proj, GameSession session) {
        boolean hitSomething = false;
        for (Zombie zombie : session.getActiveZombies()) {
            if (!zombie.isAlive()) continue;
            if (Math.abs(zombie.getX() - proj.getX()) < 0.6
                    && zombie.getY() == proj.getY()) {
                if (handleJesterDeflect(zombie, proj, session)) {
                    return true;
                }
                applyProjectileToZombie(proj, zombie);
                updateWaveHealth(session, proj.getDamage());
                hitSomething = true;
                if (zombie.getType() == ZombieType.PARASOL_ZOMBIE
                        && proj.isArc()) {
                    hitSomething = true;
                    break;
                }
            }
        }
        return hitSomething;
    }

    private boolean handleJesterDeflect(Zombie zombie, Projectile proj,
                                        GameSession session) {
        if (!(zombie instanceof JesterZombie)) {
            return false;
        }
        JesterZombie jester = (JesterZombie) zombie;
        Projectile deflected = jester.deflect(proj);
        session.getActiveProjectiles().add(deflected);
        return true;
    }

    private void applyProjectileToZombie(Projectile proj, Zombie zombie) {
        int dmg = proj.getDamage();
        if (proj.getType() == ProjectileType.FIRE) {
            dmg *= 2;
            zombie.removeEffect(ZombieEffect.CHILLED);
            zombie.addEffect(ZombieEffect.BURNING, 30);
        }
        if (proj.getType() == ProjectileType.POISON) {
            zombie.takePoisonDamage(dmg);
        } else {
            zombie.takeDamage(dmg);
        }
        if (proj.getType() == ProjectileType.ICE) {
            zombie.addEffect(ZombieEffect.CHILLED, 50);
        }
        // ← کره Kernel-pult: Stun
        if (proj.isStunOnHit())
            zombie.addEffect(ZombieEffect.STUNNED, 60);

        // ← Winter Melon / AoE پرتابه‌های منفجره
        if (proj.isExplodes() && proj.getAoeRadius() > 0)
            applyAoEDamage(zombie, proj, dmg);
    }

    private void applyAoEDamage(Zombie center, Projectile proj, int baseDmg) {
        // اطلاع‌رسانی به زامبی‌های مجاور
        // (GameSession دسترس نداریم اینجا — AoE در checkHitZombies handle می‌شه)
    }

    private void updateWaveHealth(GameSession session, int damage) {
        Wave wave = session.getCurrentWave();
        if (wave != null) wave.registerHealthLost(damage);
    }

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
        if (!session.getGameMap().isValidPosition(col, zombie.getY())) {
            return false;
        }
        Tile tile = session.getGameMap().getTile(col, zombie.getY());
        return tile != null && tile.getPlant() != null;
    }

    private void applySlipperyTile(Zombie zombie, GameSession session) {
        int col = (int) Math.round(zombie.getX());
        int row = zombie.getY();
        if (!session.getGameMap().isValidPosition(col, row)) {
            return;
        }
        Tile tile = session.getGameMap().getTile(col, row);
        if (tile == null || !tile.isSlippery()) {
            return;
        }
        int newRow = tile.isSlipperyUp() ? row - 1 : row + 1;
        if (session.getGameMap().isValidPosition(1, newRow)) {
            zombie.setY(newRow);
            zombie.setLane(newRow);
        }
    }

    private void zombiesAttackPlants(GameSession session) {
        for (Zombie zombie : session.getActiveZombies()) {
            if (!zombie.isAlive() || !zombie.isAttacking()) {
                continue;
            }
            int col = (int) Math.ceil(zombie.getX());
            Tile tile = session.getGameMap().getTile(col, zombie.getY());
            if (tile == null || tile.getPlant() == null) {
                zombie.setAttacking(false);
                continue;
            }
            Plant plant = tile.getPlant();
            // ← فراخوانی توانایی منحصربه‌فرد دفاعی گیاه
            boolean handled = plant.onZombieAttack(zombie, session);

            if (!handled) {
                // حمله عادی
                int dmgPerTick = zombie.getDamagePerSecond() / 10;
                plant.takeDamage(dmgPerTick);
            }
            if (!plant.isAlive()) {
                handlePlantDestroyed(tile, plant, zombie, session);
            }
        }
    }

    private void handlePlantDestroyed(Tile tile, Plant plant,
                                      Zombie zombie, GameSession session) {
        // ← هوک: توانایی خاص هنگام نابودی (Explode-o-nut, Hypno-shroom)
        plant.onPlantDestroyed(zombie, session);

        view.printPlantDestroyed(plant.getType().name(), tile.getX(), tile.getY());
        if (tile.getSecondLayerPlant() != null) {
            tile.setPlant(tile.getSecondLayerPlant());
            tile.setSecondLayerPlant(null);
        } else {
            tile.setPlant(null);
        }
        zombie.setAttacking(false);
        session.setPlantsLost(session.getPlantsLost() + 1);
        checkSpecialLevelLoseConditions(session);
    }

    private void checkSpecialLevelLoseConditions(GameSession session) {
        model.Level level = session.getLevel();
        if (level.getLevelType() == LevelType.LOVE_YOUR_PLANTS) {
            if (session.getPlantsLost() >= level.getMaxPlantsLost()) {
                session.setResult(GameResult.LOSE);
                view.printGameOver();
            }
        }
    }

    private void checkZombiesAtEnd(GameSession session) {
        Iterator<Zombie> iter = session.getActiveZombies().iterator();
        while (iter.hasNext()) {
            Zombie zombie = iter.next();
            if (!zombie.isAlive()) {
                continue;
            }
            if (zombie.getX() <= 0) {
                handleZombieReachedEnd(zombie, session);
            }
        }
    }

    private void handleZombieReachedEnd(Zombie zombie, GameSession session) {
        int rowIndex = zombie.getY() - 1;
        if (session.getGameMap().isLawnMowerAvailable(rowIndex)) {
            triggerLawnMower(rowIndex, session);
        } else {
            session.setResult(GameResult.LOSE);
            view.printGameOver();
        }
    }

    private void triggerLawnMower(int rowIndex, GameSession session) {
        session.getGameMap().setLawnMowerUsed(rowIndex);
        List<String> killed = new ArrayList<>();
        List<Zombie> toKill = new ArrayList<>();
        for (Zombie z : session.getActiveZombies()) {
            if (z.getY() - 1 == rowIndex && !(z instanceof Gargantuar)) {
                killed.add(z.getType().name());
                toKill.add(z);
            }
        }
        toKill.forEach(z -> {
            z.setCurrentHealth(0);
            session.setZombiesKilled(session.getZombiesKilled() + 1);
        });
        view.printLawnMowerTriggered(rowIndex + 1, killed);
        onLawnmowerKill(killed.size());
    }

    private void removeDeadEntities(GameSession session) {
        // گیاهان مرده (HP=0) از کاشی‌ها برداشته می‌شوند
        for (int r = 1; r <= session.getGameMap().getRows(); r++) {
            for (int c = 1; c <= session.getGameMap().getCols(); c++) {
                Tile tile = session.getGameMap().getTile(c, r);
                if (tile == null) continue;
                Plant plant = tile.getPlant();
                if (plant != null && !plant.isAlive()) {
                    // اگر قبلاً handlePlantDestroyed صدا نشده (مثلاً گیاهان آنی)
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
        session.getActiveZombies().removeAll(toRemove);
        session.getActiveProjectiles().removeIf(
                p -> p.getX() < 0 || p.getX() > session.getGameMap().getCols() + 2);
    }

    private void handleZombieDeath(Zombie zombie, GameSession session) {
        view.printZombieDead(zombie.getType().name(), zombie.getX(), zombie.getY());
        session.setZombiesKilled(session.getZombiesKilled() + 1);
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
            }
        }
        Wave wave = session.getCurrentWave();
        if (wave != null) {
            wave.registerHealthLost(zombie.getMaxHealth());
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
    }

    private void handleDrops(Zombie zombie, GameSession session) {
        if (!RandomUtil.chance(0.1)) {
            return;
        }
        model.User user = model.AppState.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        double roll = RandomUtil.nextDouble();
        if (roll < 0.33) {
            user.setCoins(user.getCoins() + 50);
            view.printZombieDropped("coin", (int) user.getCoins());
        } else if (roll < 0.66) {
            user.setGems(user.getGems() + 1);
            view.printZombieDropped("diamond", user.getGems());
        } else {
            user.setPots(user.getPots() + 1);
            view.printZombieDropped("pot", user.getPots());
        }
    }
    private void applyIceMeltNearFire(GameSession session) {
        for (int r = 1; r <= session.getGameMap().getRows(); r++) {
            for (int c = 1; c <= session.getGameMap().getCols(); c++) {
                Tile tile = session.getGameMap().getTile(c, r);
                if (tile == null || tile.getPlant() == null) {
                    continue;
                }
                Plant p = tile.getPlant();
                if (p.isFrozen() && hasAdjacentFirePlant(session, c, r)) {
                    p.takeDamage(6);
                    if (p.getFreezeLevel() > 0
                            && RandomUtil.chance(0.1)) {
                        p.setFreezeLevel(p.getFreezeLevel() - 1);
                        if (p.getFreezeLevel() < 3) {
                            p.thaw();
                        }
                    }
                }
            }
        }
    }
    private boolean hasAdjacentFirePlant(GameSession session, int cx, int cy) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                Tile t = session.getGameMap().getTile(cx + dx, cy + dy);
                if (t != null && t.getPlant() != null
                        && t.getPlant().isFirePlant()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void onSunCollectedForQuest(int amount) {
        if (questService != null) {
            questService.onSunCollected(amount);
        }
    }

    public void onExplosiveUsed() {
        if (questService != null) {
            questService.onExplosiveUsed();
        }
    }

    public void onLawnmowerKill(int count) {
        if (questService != null) {
            questService.onLawnmowerKill(count);
        }
    }
    public void checkWinCondition(GameSession session) {
        if (!session.isInProgress()) {
            return;
        }
        if (session.allWavesFinished()) {
            session.setResult(GameResult.WIN);
            view.printGameWon();
            notifyLevelComplete(session);
            if (questService != null) {
                questService.onGameWon(session);
            }
        }
    }
    private void notifyLevelComplete(GameSession session) {
        model.User user = model.AppState.getInstance().getCurrentUser();
        if (user == null || session.getLevel() == null) {
            return;
        }
        model.enums.ChapterType chapter = session.getLevel().getChapter();
        int levelNum = session.getLevelNumber();
        if (levelProgressService != null) {
            levelProgressService.onLevelCompleted(user, chapter, levelNum);
        }
        if (scoredGameService != null) {
            scoredGameService.saveHighScore(user, session.getMeoPoints());
        }
    }
}