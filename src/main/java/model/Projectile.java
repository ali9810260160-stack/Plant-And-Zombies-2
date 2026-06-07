package model;

import model.enums.ProjectileType;

/**
 * یک پرتابه در حال حرکت روی نقشه.
 * توسط گیاهان شلیک می‌شود و به زامبی‌ها آسیب می‌رساند.
 */
public class Projectile {

    /** نوع پرتابه */
    private ProjectileType type;

    /** موقعیت افقی فعلی (اعشاری) */
    private double x;

    /** ردیف */
    private int y;

    /** مقدار آسیب */
    private int damage;

    /** سرعت حرکت (خانه بر ثانیه) */
    private double speed;

    /** آیا این پرتابه هوایی است (lobber) */
    private boolean lobbed;

    /** مختصات هدف (برای پرتابه‌های lobber) */
    private int targetX;
    private int targetY;

    /** آیا این پرتابه نفوذکننده است (از چند زامبی رد می‌شود) */
    private boolean strikeThrough;

    public Projectile(ProjectileType type, double x, int y, int damage) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.damage = damage;
    }

    /** یک تیک پرتابه را جلو می‌برد */
    public void advance(double deltaX) { }

    /** بررسی می‌کند آیا پرتابه از نقشه خارج شده */
    public boolean isOutOfBounds(int mapCols) { return false; }

    public ProjectileType getType() { return type; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public int getY() { return y; }
    public int getDamage() { return damage; }
    public double getSpeed() { return speed; }
    public boolean isLobbed() { return lobbed; }
    public void setLobbed(boolean lobbed) { this.lobbed = lobbed; }
    public int getTargetX() { return targetX; }
    public void setTargetX(int targetX) { this.targetX = targetX; }
    public int getTargetY() { return targetY; }
    public void setTargetY(int targetY) { this.targetY = targetY; }
    public boolean isStrikeThrough() { return strikeThrough; }
    public void setStrikeThrough(boolean strikeThrough) { this.strikeThrough = strikeThrough; }
}
