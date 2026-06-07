package model.zombies;

import model.enums.ZombieType;

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
    }

    /**
     * وقتی پرتابه به سمتش می‌آید شروع به چرخش می‌کند.
     * پرتابه برگردانده می‌شود.
     */
    public void startSpinning() { }

    /**
     * وقتی پرتابه‌ای نیامد، چرخش متوقف می‌شود.
     */
    public void stopSpinning() { }

    public boolean isSpinning() { return spinning; }

    @Override
    public void onTick(int tickCount) { }

    @Override
    public String getDescription() {
        return "Jester Zombie: Deflects projectiles back at your plants while spinning.";
    }
}
