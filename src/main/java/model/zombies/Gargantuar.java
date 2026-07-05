package model.zombies;

import model.GameMap;
import model.GameSession;
import model.enums.ChapterType;
import model.enums.ZombieType;
import model.plants.Plant;
import model.tiles.Tile;

import java.util.LinkedHashMap;

import static model.enums.ZombieType.DRAGON_IMP;
import static model.enums.ZombieType.IMP;

/**
 * زامبی غول‌پیکر - قوی‌ترین زامبی معمولی.
 * با یک ضربه گیاه را از بین می‌برد.
 * وقتی به نصف HP رسید، imp را پرتاب می‌کند.
 */
public class Gargantuar extends Zombie {

    /** آیا imp را قبلاً پرتاب کرده */
    private boolean impThrown;

    /** imp پشت این گارگانتوار */
    private ImpZombie carriedImp;

    public Gargantuar() {
        this.type = ZombieType.GARGANTUAR;
        this.maxHealth = 3600;
        this.currentHealth = 3600;
        this.moveSpeed = 0.24;
        this.damagePerSecond = 1500;
        this.waveCost = 1500;
        this.boss = true;
        this.impThrown = false;
        this.carriedImp =new ImpZombie(IMP,0.22,100);
        this.armors = new LinkedHashMap<>();
        this.activeEffects = new LinkedHashMap<>();
    }

    @Override
    public void onTick(int tickCount, GameSession gameSession) {
        if(!isAlive()) return;
        GameMap map = gameSession.getGameMap();
        Tile tile = map.getTile((int)x,y);
        if(tile != null && tile.getPlant() != null){
            Plant plant = tile.getPlant();
            plant.setCurrentHealth(0);
            tile.setPlant(null);
        }

        if(currentHealth <= maxHealth/2 && !impThrown){
            throwImp(gameSession);
        }
    }

    /**
     * imp را به ستون سوم از چپ همان ردیف پرتاب می‌کند.
     * وقتی HP به نصف رسید فراخوانی می‌شود.
     * در فصل قرون‌وسطی، ایمپ پرتاب‌شده از نوع ایمپ اژدها (مقاوم به آتش) خواهد بود.
     */
    public void throwImp(GameSession gameSession) {
        GameMap map = gameSession.getGameMap();
        if (map.getChapter() == ChapterType.DARK_AGES) {
            carriedImp = new ImpZombie(DRAGON_IMP, 0.185, 150);
        }
        carriedImp.setX(3);
        carriedImp.setY(y);
        gameSession.getActiveZombies().add(carriedImp);
        Tile targetTile = map.getTile(3, y);
        if (targetTile != null && targetTile.getZombiesOnTile() != null) {
            targetTile.getZombiesOnTile().add(carriedImp);
        }
        impThrown = true;
    }

    public boolean isImpThrown() { return impThrown; }
    public void setImpThrown(boolean impThrown) { this.impThrown = impThrown; }
    public ImpZombie getCarriedImp() { return carriedImp; }

    @Override
    public String getDescription() {
        return "Gargantuar: Smashes plants with a telephone pole. Very tough.";
    }
}
