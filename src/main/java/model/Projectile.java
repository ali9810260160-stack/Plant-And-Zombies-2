package model;

import model.enums.ProjectileType;

/**
 * مدل پرتابه در حال حرکت روی نقشه.
 */
public class Projectile {

    private ProjectileType type;
    private double x;
    private int y;
    private double speed;
    private int damage;
    private boolean movingRight;
    private boolean passesThrough;
    private boolean isArc;
    private int targetX;
    private int targetY;
    private boolean explodes;
    private int aoeRadius;
    private boolean hitsPlants;

    public Projectile(ProjectileType type, double x, int y, int damage) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.damage = damage;
        this.speed = 3.0;
        this.movingRight = false;
        this.passesThrough = false;
        this.isArc = false;
        this.explodes = false;
        this.aoeRadius = 0;
        this.hitsPlants = false;
    }

    public ProjectileType getType() { return type; }
    public void setType(ProjectileType t) { this.type = t; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }
    public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }
    public boolean isMovingRight() { return movingRight; }
    public void setMovingRight(boolean movingRight) { this.movingRight = movingRight; }
    public boolean isPassesThrough() { return passesThrough; }
    public void setPassesThrough(boolean passesThrough) { this.passesThrough = passesThrough; }
    public boolean isArc() { return isArc; }
    public void setArc(boolean arc) { isArc = arc; }
    public int getTargetX() { return targetX; }
    public void setTargetX(int targetX) { this.targetX = targetX; }
    public int getTargetY() { return targetY; }
    public void setTargetY(int targetY) { this.targetY = targetY; }
    public boolean isExplodes() { return explodes; }
    public void setExplodes(boolean explodes) { this.explodes = explodes; }
    public int getAoeRadius() { return aoeRadius; }
    public void setAoeRadius(int aoeRadius) { this.aoeRadius = aoeRadius; }
    public boolean isHitsPlants() { return hitsPlants; }
    public void setHitsPlants(boolean hitsPlants) { this.hitsPlants = hitsPlants; }
}
