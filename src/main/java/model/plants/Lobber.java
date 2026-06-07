package model.plants;

/**
 * کلاس انتزاعی برای گیاهان پرتاب‌کننده هوایی (مثل منجنیق).
 * پرتابه‌های این گیاهان موانع را نادیده می‌گیرند.
 * شامل CabbagePult, MelonPult, KernelPult و ...
 */
public abstract class Lobber extends Plant {

    /** مقدار آسیب هر پرتابه */
    protected int damage;

    /** تیک‌های بین دو شلیک */
    protected int attackIntervalTicks;

    /** آیا این lobber روی زامبیِ هدف قفل می‌کند یا تصادفی پرتاب می‌کند */
    protected boolean targetsNearestZombie;

    /** پرتابه هوایی ایجاد می‌کند */
    public abstract void lob();

    public int getDamage() { return damage; }
    public int getAttackIntervalTicks() { return attackIntervalTicks; }
    public boolean isTargetsNearestZombie() { return targetsNearestZombie; }
}
