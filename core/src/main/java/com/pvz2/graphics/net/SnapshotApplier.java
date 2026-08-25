package com.pvz2.graphics.net;

import com.pvz2.graphics.GameStateSnapshot;
import com.pvz2.model.GameSession;
import com.pvz2.model.Projectile;
import com.pvz2.model.Sun;
import com.pvz2.model.enums.PlantType;
import com.pvz2.model.enums.ProjectileType;
import com.pvz2.model.enums.SunType;
import com.pvz2.model.enums.ZombieEffect;
import com.pvz2.model.enums.ZombieType;
import com.pvz2.model.plants.Plant;
import com.pvz2.model.plants.PlantFactory;
import com.pvz2.model.tiles.Tile;
import com.pvz2.model.zombies.Zombie;
import com.pvz2.model.zombies.ZombieFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Guest-side reconciler for the host-authoritative snapshot model (phase 3).
 *
 * <p>Mutates a "mirror" {@link GameSession} so its live model objects match the
 * host's {@link GameStateSnapshot}, letting the existing {@code AnimationSystem}
 * render an identical picture. Zombies are matched by stable {@code netId} so
 * their walk animation stays continuous across frames; plants are matched by
 * cell; projectiles and suns (short-lived) are rebuilt each frame.
 */
public final class SnapshotApplier {

    /** host netId → mirror Zombie, so controllers persist across frames. */
    private final Map<Integer, Zombie> zombiesById = new HashMap<>();

    public void apply(GameSession mirror, GameStateSnapshot snap) {
        if (mirror == null || snap == null || mirror.getGameMap() == null) return;
        applyZombies(mirror, snap);
        applyPlants(mirror, snap);
        applyProjectiles(mirror, snap);
        applySuns(mirror, snap);
    }

    // ─── zombies (matched by netId) ────────────────────────────────────────────
    private void applyZombies(GameSession mirror, GameStateSnapshot snap) {
        Set<Integer> present = new HashSet<>();
        for (GameStateSnapshot.ZombieInfo zi : snap.zombies) {
            present.add(zi.netId);
            Zombie z = zombiesById.get(zi.netId);
            if (z == null) {
                ZombieType type = parseZombie(zi.type);
                if (type == null) continue;
                z = ZombieFactory.create(type);
                if (z == null) continue;
                z.setNetId(zi.netId);
                zombiesById.put(zi.netId, z);
                mirror.getActiveZombies().add(z);
            }
            z.setX(zi.phase1X);
            z.setY(zi.phase1Y);
            z.setLane(zi.phase1Y);
            z.setCurrentHealth((int) zi.hp);
            z.setAttacking(zi.isAttacking);
            z.setGlowing(zi.isGlowing);
            z.setMovingBackward(zi.movingBackward);
            z.setHypnotized(zi.isHypnotized);
            applyEffects(z, zi.effects);
        }
        // remove zombies no longer in the snapshot
        Iterator<Map.Entry<Integer, Zombie>> it = zombiesById.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Zombie> e = it.next();
            if (!present.contains(e.getKey())) {
                mirror.getActiveZombies().remove(e.getValue());
                it.remove();
            }
        }
    }

    private void applyEffects(Zombie z, java.util.List<String> effects) {
        Set<ZombieEffect> want = new HashSet<>();
        if (effects != null) {
            for (String s : effects) {
                try { want.add(ZombieEffect.valueOf(s.toUpperCase())); }
                catch (IllegalArgumentException ignored) { }
            }
        }
        for (ZombieEffect ef : ZombieEffect.values()) {
            boolean has = z.hasEffect(ef);
            if (want.contains(ef) && !has) z.addEffect(ef, 100000);
            else if (!want.contains(ef) && has) z.removeEffect(ef);
        }
    }

    // ─── plants (matched by cell) ──────────────────────────────────────────────
    private void applyPlants(GameSession mirror, GameStateSnapshot snap) {
        int cols = mirror.getGameMap().getCols();
        int rows = mirror.getGameMap().getRows();
        Set<Long> firstLayer = new HashSet<>();
        Set<Long> secondLayer = new HashSet<>();
        for (GameStateSnapshot.PlantInfo pi : snap.plants) {
            Tile tile = mirror.getGameMap().getTile(pi.phase1X, pi.phase1Y);
            if (tile == null) continue;
            PlantType type = parsePlant(pi.type);
            if (type == null) continue;
            long key = ((long) pi.phase1X << 8) | pi.phase1Y;
            if (pi.isOverlay) {
                secondLayer.add(key);
                Plant existing = tile.getSecondLayerPlant();
                if (existing == null || existing.getType() != type) {
                    existing = makePlant(type, pi.phase1X, pi.phase1Y);
                    tile.setSecondLayerPlant(existing);
                }
                updatePlant(existing, pi);
            } else {
                firstLayer.add(key);
                Plant existing = tile.getPlant();
                if (existing == null || existing.getType() != type) {
                    existing = makePlant(type, pi.phase1X, pi.phase1Y);
                    tile.setPlant(existing);
                }
                updatePlant(existing, pi);
            }
        }
        // clear cells whose plant vanished
        for (int c = 1; c <= cols; c++) {
            for (int r = 1; r <= rows; r++) {
                Tile tile = mirror.getGameMap().getTile(c, r);
                if (tile == null) continue;
                long key = ((long) c << 8) | r;
                if (tile.getPlant() != null && !firstLayer.contains(key)) tile.setPlant(null);
                if (tile.getSecondLayerPlant() != null && !secondLayer.contains(key)) {
                    tile.setSecondLayerPlant(null);
                }
            }
        }
    }

    private Plant makePlant(PlantType type, int x, int y) {
        Plant p = PlantFactory.create(type);
        if (p != null) { p.setX(x); p.setY(y); }
        return p;
    }

    private void updatePlant(Plant p, GameStateSnapshot.PlantInfo pi) {
        if (p == null) return;
        p.setCurrentHealth((int) pi.hp);
        p.setFreezeLevel(pi.freezeLevel);
    }

    // ─── projectiles + suns (rebuilt each frame) ───────────────────────────────
    private void applyProjectiles(GameSession mirror, GameStateSnapshot snap) {
        mirror.getActiveProjectiles().clear();
        for (GameStateSnapshot.ProjectileInfo pi : snap.projectiles) {
            ProjectileType type = parseProjectile(pi.type);
            if (type == null) continue;
            Projectile pr = new Projectile(type, pi.phase1X, pi.phase1Y, 0);
            pr.setMovingRight(pi.movingRight);
            pr.setArc(pi.isArc);
            pr.setTargetX(pi.targetPhase1X);
            pr.setTargetY(pi.targetPhase1Y);
            mirror.getActiveProjectiles().add(pr);
        }
    }

    private void applySuns(GameSession mirror, GameStateSnapshot snap) {
        mirror.getActiveSuns().clear();
        for (GameStateSnapshot.SunInfo si : snap.sunItems) {
            SunType type = parseSun(si.sunType);
            if (type == null) type = SunType.NORMAL;
            Sun sun = new Sun(type, si.phase1X, si.phase1Y, 0);
            sun.setLanded(si.isLanded);
            sun.setFallProgress(si.fallProgress);
            sun.setValue(si.value);
            mirror.getActiveSuns().add(sun);
        }
    }

    /** Reset all mirror bookkeeping (on match end / screen dispose). */
    public void reset() { zombiesById.clear(); }

    // ─── enum parsing (snapshot stores lowercase names) ────────────────────────
    private static ZombieType parseZombie(String s) { return parse(ZombieType.class, s); }
    private static PlantType parsePlant(String s) { return parse(PlantType.class, s); }
    private static ProjectileType parseProjectile(String s) { return parse(ProjectileType.class, s); }
    private static SunType parseSun(String s) { return parse(SunType.class, s); }

    private static <E extends Enum<E>> E parse(Class<E> cls, String s) {
        if (s == null) return null;
        try { return Enum.valueOf(cls, s.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }
}
