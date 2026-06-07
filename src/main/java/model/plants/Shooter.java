package model.plants;

import model.enums.ProjectileType;

/**
 * کلاس انتزاعی برای گیاهان شلیک‌کننده مستقیم.
 * شامل Peashooter, Repeater, SnowPea و ...
 */
public abstract class Shooter extends Plant {

    /** نوع پرتابه شلیک‌شده */
    protected ProjectileType projectileType;

    /** مقدار آسیب هر پرتابه */
    protected int damage;

    /** تعداد پرتابه در هر شلیک */
    protected int projectilesPerShot;

    /** تیک‌های بین دو شلیک */
    protected int attackIntervalTicks;

    /** تیک‌های گذشته از آخرین شلیک */
    protected int ticksSinceLastShot;

    /** بررسی وجود زامبی در ردیف برای شلیک */
    public abstract boolean hasTargetInLane();

    /** پرتابه ایجاد می‌کند و به سیستم بازی اضافه می‌کند */
    public abstract void shoot();

    public ProjectileType getProjectileType() { return projectileType; }
    public int getDamage() { return damage; }
    public int getProjectilesPerShot() { return projectilesPerShot; }
    public int getAttackIntervalTicks() { return attackIntervalTicks; }
}
