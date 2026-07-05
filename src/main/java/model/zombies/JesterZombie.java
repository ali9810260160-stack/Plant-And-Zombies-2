package model.zombies;

import model.GameSession;
import model.Projectile;
import model.enums.ZombieType;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * ژانگولر - تیرهای مستقیم را به سمت گیاهان برمی‌گرداند.
 * حین چرخش سریع‌تر حرکت می‌کند.
 * تیرهای یخی برگشتی سبب یخ‌زدن گیاهان می‌شوند.
 */
public class JesterZombie extends Zombie {

    /** آیا در حال چرخش است */
    private boolean spinning;

    /** سرعت عادی (قبل از چرخش) */
    private final double normalSpeed = 0.2;

    /** سرعت حین چرخش */
    private final double spinSpeed = 0.6;

    public JesterZombie() {
        this.type = ZombieType.JESTER_ZOMBIE;
        this.maxHealth = 500;
        this.currentHealth = 500;
        this.moveSpeed = normalSpeed;
        this.damagePerSecond = 100;
        this.waveCost = 5;
        this.spinning = false;
        this.armors = new LinkedHashMap<>();
        this.activeEffects = new LinkedHashMap<>();
    }

    private boolean isProjectileComing(GameSession gameSession){
        boolean isprojectilecoming = false;
        ArrayList<Projectile> projectiles = new ArrayList<>();
        for(Projectile projectile : gameSession.getActiveProjectiles()){
            if(projectile.isLobbed() && projectile.getTargetX() == (int)x && projectile.getTargetY()==y){
                projectiles.add(projectile);
                isprojectilecoming = true;
            }else if(!projectile.isLobbed() && projectile.getTargetX() <= x && projectile.getTargetY() == y){
                projectiles.add(projectile);
                isprojectilecoming = true;
            }
        }
        return isprojectilecoming;
    }
    /**
     * وقتی پرتابه به سمتش می‌آید شروع به چرخش می‌کند.
     * پرتابه برگردانده می‌شود.
     */
    public void startSpinning() {
        spinning = true;
        moveSpeed = spinSpeed;
    }

    /**
     * وقتی پرتابه‌ای نیامد، چرخش متوقف می‌شود.
     */
    public void stopSpinning() {
        spinning = false;
        moveSpeed = normalSpeed;
    }

    public boolean isSpinning() { return spinning; }

    @Override
    public void onTick(int tickCount, GameSession gameSession) {
        if(isProjectileComing(gameSession) && !spinning){
            startSpinning();
        }
        if(!isProjectileComing(gameSession) && spinning){
            stopSpinning();
        }
    }

    @Override
    public String getDescription() {
        return "Jester Zombie: Deflects projectiles back at your plants while spinning.";
    }
}
