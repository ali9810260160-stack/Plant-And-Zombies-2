package model.zombies;

import model.GameSession;
import model.enums.ZombieType;

/**
 * زامبی پایه - ساده‌ترین نوع زامبی با منطق حرکت و حمله.
 */
public class NormalZombie extends Zombie {

    public static final double MAX_ALLSTAR_SPEED = 0.5;

    private int specialTimer;

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
                handleAllStar();
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
            default:
                break;
        }
    }

    private void handleNewspaper() {
        if (!hasArmor(model.enums.ArmorType.NEWSPAPER)
                && moveSpeed < 0.4) {
            moveSpeed = 0.4;
        }
    }

    private void handleAllStar() {
        if (moveSpeed > 0.16) {
            moveSpeed = MAX_ALLSTAR_SPEED;
        }
    }

    private void handleTurquoise(int tick, GameSession session) {
        if (specialTimer % 10 == 0) {
            boolean hasNearbyPlant = checkNearbyPlant(session);
            if (hasNearbyPlant && specialTimer <= 50) {
                int stolen = Math.min(25, session.getSunAmount());
                session.setSunAmount(session.getSunAmount() - stolen);
            }
        }
    }

    private boolean checkNearbyPlant(GameSession session) {
        for (int dx = -4; dx <= 0; dx++) {
            int col = (int) x + dx;
            if (session.getGameMap().isValidPosition(col, y)) {
                if (session.getGameMap().getTile(col, y).getPlant() != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private void handleProspector(int tick, GameSession session) {
        if (specialTimer == 100 && !movingBackward) {
            movingBackward = true;
            x = session.getGameMap().getCols();
        }
    }

    private void handleKing(int tick, GameSession session) {
        if (specialTimer % 50 == 0) {
            upgradeNearbyZombies(session);
        }
    }

    private void upgradeNearbyZombies(GameSession session) {
        for (Zombie z : session.getActiveZombies()) {
            if (z.getType() == ZombieType.NORMAL
                    && Math.abs(z.getX() - x) <= 2
                    && Math.abs(z.getY() - y) <= 1) {
                z.addArmor(model.enums.ArmorType.HELMET, 1600);
                z.addArmor(model.enums.ArmorType.SHOULDER_ARMOR, 1600);
            }
        }
    }

    private void handleRaZombie(int tick, GameSession session) {
        if (specialTimer % 10 == 0) {
            stealNearestGroundSun(session);
        }
    }

    private void stealNearestGroundSun(GameSession session) {
        if (session.getActiveSuns() == null) {
            return;
        }
        session.getActiveSuns().removeIf(sun -> {
            if (sun.isLanded() && !sun.isCollected()
                    && Math.abs(sun.getX() - (int) x) <= 2
                    && sun.getY() == y) {
                return true;
            }
            return false;
        });
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

    @Override
    public String getDescription() {
        return type.name() + " - a zombie with "
                + maxHealth + " HP and "
                + damagePerSecond + " DPS.";
    }
}
