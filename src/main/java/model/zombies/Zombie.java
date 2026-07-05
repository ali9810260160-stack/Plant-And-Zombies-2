package model.zombies;

import model.GameMap;
import model.GameSession;
import model.enums.ArmorType;
import model.enums.ZombieEffect;
import model.enums.ZombieType;
import model.tiles.Tile;

import java.util.Map;

import static model.zombies.NormalZombie.MAX_ALLSTAR_SPEED;

/**
 * کلاس انتزاعی پایه برای تمام زامبی‌ها.
 * شامل مشخصات ذاتی، زره، موقعیت و افکت‌های فعال.
 */
public abstract class Zombie {

    /** نوع زامبی */
    protected ZombieType type;

    /** سلامتی فعلی */
    protected int currentHealth;

    /** حداکثر سلامتی */
    protected int maxHealth;

    /** سرعت حرکت (خانه بر ثانیه) */
    protected double moveSpeed;

    /** آسیب دمیج به گیاه در هر ثانیه */
    protected int damagePerSecond;

    /** هزینه موج (waveCost) برای محاسبه سختی موج */
    protected int waveCost;

    /** موقعیت افقی (می‌تواند اعشاری باشد) */
    protected double x;

    /** موقعیت ردیف (صحیح) */
    protected int y;

    /** آیا این زامبی درخشان است (5% احتمال، plant food می‌دهد) */
    protected boolean glowing;

    /** آیا این زامبی رئیس است (ماشین چمن‌زنی را فعال نمی‌کند) */
    protected boolean boss;

    /** زره‌های فعال با مقدار HP هر کدام */
    protected Map<ArmorType, Integer> armors;

    /** افکت‌های وضعیتی فعال با تیک باقیمانده */
    protected Map<ZombieEffect, Integer> activeEffects;

    /** شماره موج که این زامبی در آن ظاهر شده */
    protected int spawnWave;

    /** شماره ردیف که این زامبی در آن حرکت می‌کند */
    protected int lane;

    /**
     * آیا این زامبی خلاف جهت عادی (به سمت راست) حرکت می‌کند.
     * مثلاً پراسپکتور بعد از انفجار دینامیتش.
     */
    protected boolean movingBackward;


    // ---- Abstract Methods ----

    /** رفتار خاص این زامبی را در هر تیک اجرا می‌کند */
    public abstract void onTick(int tickCount, GameSession gameSession);

    /** رشته توضیحات این زامبی را برمی‌گرداند (برای collection) */
    public abstract String getDescription();

    /**
     * هنگام مرگ این زامبی فراخوانی می‌شود تا اثرات ویژه مرگ (مثل بازگرداندن خورشیدهای دزدیده‌شده
     * یا آزاد کردن گیاهان طلسم‌شده) اعمال شود. پیاده‌سازی پیش‌فرض کاری انجام نمی‌دهد.
     */
    public void onDeath(GameSession gameSession) {
    }

    // ---- Common Methods ----

    /** آسیب وارد می‌کند - ابتدا به زره، بعد به خود زامبی */
    public void takeDamage(int damage) {
        if(!isAlive()) return;

        if(!armors.isEmpty()){
            int remainDamage = damageToArmor(damage);
            if(remainDamage == 0) return;
            else{
                currentHealth = Math.max(0,currentHealth-damage);
                return;
            }
        }

        currentHealth = Math.max(0,currentHealth-damage);
    }




    private int damageToArmor(int damage){

        if(armors.size() == 2){
            int armorHP = armors.get(ArmorType.HELMET);
            int remainDamage = damage - armorHP;
            armorHP = Math.max(0,armorHP-damage);

            if(armorHP == 0) {
                if(remainDamage == 0) return 0;

                removeArmor(ArmorType.HELMET);
                armorHP = armors.get(ArmorType.SHOULDER_ARMOR);
                armorHP = Math.max(0,armorHP-remainDamage);

                if(armorHP == 0){
                    remainDamage = remainDamage - armors.get(ArmorType.SHOULDER_ARMOR);
                    removeArmor(ArmorType.SHOULDER_ARMOR);
                    return remainDamage;
                }else{
                    armors.put(ArmorType.SHOULDER_ARMOR,armorHP);
                    return 0;
                }

            }else{
                armors.put(ArmorType.HELMET,armorHP);
                return 0;
            }
        }


        if(hasArmor(ArmorType.BUCKET)){
            return damageToOneArmor(ArmorType.BUCKET,damage);
        }else if(hasArmor(ArmorType.CONE)){
            return damageToOneArmor(ArmorType.CONE,damage);
        }else if (hasArmor(ArmorType.BLOCK)){
            return damageToOneArmor(ArmorType.BLOCK,damage);
        }else if(hasArmor(ArmorType.SHOULDER_ARMOR)){
            return damageToOneArmor(ArmorType.SHOULDER_ARMOR,damage);
        }else{
            return damageToOneArmor(ArmorType.NEWSPAPER,damage);
        }

    }

    private int damageToOneArmor(ArmorType armorType, int damage){
        int armorHP = armors.get(armorType);
        armorHP = Math.max(0,armorHP-damage);
        if(armorHP == 0){
            int remainDamage = damage - armors.get(armorType);
            removeArmor(armorType);
            return remainDamage;
        }else{
            armors.put(armorType,armorHP);
            return 0;
        }
    }

    /** آسیب سمی وارد می‌کند - زره را نادیده می‌گیرد */
    public void takePoisonDamage(int damage) {
        currentHealth = Math.max(0,currentHealth-damage);
    }

    /** بررسی می‌کند آیا زامبی زنده است */
    public boolean isAlive() { return currentHealth > 0; }

    /** بررسی می‌کند آیا افکت خاصی فعال است */
    public boolean hasEffect(ZombieEffect effect) {
        return activeEffects.containsKey(effect);
    }

    /** یک افکت اضافه می‌کند */
    public void addEffect(ZombieEffect effect, int durationTicks) {
        activeEffects.put(effect,durationTicks);
    }

    /** یک افکت حذف می‌کند */
    public void removeEffect(ZombieEffect effect) {
        if(hasEffect(effect)) activeEffects.remove(effect);
        else return;
    }

    /** سرعت مؤثر را با احتساب افکت‌ها برمی‌گرداند */
    public double getEffectiveMoveSpeed() {
        if(hasEffect(ZombieEffect.CHILLED) || hasEffect(ZombieEffect.SLOWED)){
            return moveSpeed/2;
        }
        return moveSpeed;
    }

    /** یک نوع زره را حذف می‌کند (وقتی HP آن صفر شد) */
    public void removeArmor(ArmorType armorType) {
        if(hasArmor(armorType)) armors.remove(armorType);
        else return;
    }

    /** بررسی می‌کند آیا زره خاصی دارد */
    public boolean hasArmor(ArmorType armorType) {
        return armors.containsKey(armorType);
    }

    public void move(GameSession gameSession){
        if(!isAlive()) return;

        if (activeEffects != null
                && (activeEffects.containsKey(ZombieEffect.FROZEN)
                || activeEffects.containsKey(ZombieEffect.STUNNED))) {
            return;
        }

        if(isAttacking(gameSession)) return;

        double effectiveMoveSpeed = getEffectiveMoveSpeed();


        double deltaX = effectiveMoveSpeed * 0.1f;
        if(movingBackward) x += deltaX;
        else x -= deltaX;
    }


    //ایا الان زامبی در حال حمله ست یا نه
    public boolean isAttacking(GameSession gameSession){
        GameMap map = gameSession.getGameMap();
        Tile tile = map.getTile((int)x,y);
        if(tile.getPlant() == null) return false;
        else return true;
    }

    // ---- Getters & Setters ----

    public ZombieType getType() { return type; }
    public int getCurrentHealth() { return currentHealth; }
    public void setCurrentHealth(int currentHealth) { this.currentHealth = currentHealth; }
    public int getMaxHealth() { return maxHealth; }
    public double getMoveSpeed() { return moveSpeed; }
    public void setMoveSpeed(double moveSpeed) { this.moveSpeed = moveSpeed; }
    public int getDamagePerSecond() { return damagePerSecond; }
    public int getWaveCost() { return waveCost; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public boolean isGlowing() { return glowing; }
    public void setGlowing(boolean glowing) { this.glowing = glowing; }
    public boolean isBoss() { return boss; }
    public int getSpawnWave() { return spawnWave; }
    public void setSpawnWave(int spawnWave) { this.spawnWave = spawnWave; }
    public int getLane() { return lane; }
    public void setLane(int lane) { this.lane = lane; }
    public Map<ArmorType, Integer> getArmors() { return armors; }
    public Map<ZombieEffect, Integer> getActiveEffects() { return activeEffects; }
}
