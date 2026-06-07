package service;

import model.GameSession;
import model.Projectile;
import model.plants.Plant;
import model.zombies.Zombie;

/**
 * سرویس مبارزه.
 * مسئول پردازش پرتابه‌ها، برخوردها، آسیب و تعاملات خاص.
 */
public class CombatService {

    /**
     * تمام پرتابه‌های فعال را یک تیک جلو می‌برد و برخوردها را پردازش می‌کند.
     * @param session session جاری
     */
    public void processProjectiles(GameSession session) { }

    /**
     * تمام زامبی‌های فعال را یک تیک جلو می‌برد.
     * حرکت، حمله به گیاه، و رسیدن به انتها را مدیریت می‌کند.
     * @param session session جاری
     */
    public void processZombies(GameSession session) { }

    /**
     * تمام گیاهان فعال را یک تیک پردازش می‌کند.
     * شلیک، تولید خورشید، cooldown و افکت‌ها.
     * @param session session جاری
     */
    public void processPlants(GameSession session) { }

    /**
     * برخورد پرتابه با زامبی را پردازش می‌کند.
     * @param session session جاری
     * @param projectile پرتابه
     * @param zombie زامبی هدف
     */
    public void applyProjectileHit(GameSession session, Projectile projectile,
                                   Zombie zombie) { }

    /**
     * آسیب مساحتی (AoE) را در یک مربع روی نقشه اعمال می‌کند.
     * @param session session جاری
     * @param cx مرکز x
     * @param cy مرکز y
     * @param radius شعاع
     * @param damage مقدار آسیب
     * @param hitsZombies آیا به زامبی آسیب می‌زند
     * @param hitsPlants آیا به گیاه آسیب می‌زند
     */
    public void applyAoeDamage(GameSession session, int cx, int cy, int radius,
                               int damage, boolean hitsZombies, boolean hitsPlants) { }

    /**
     * زامبی را هیپنوتیزم می‌کند (به سمت زامبی‌های دیگر حمله می‌کند).
     * @param session session جاری
     * @param zombie زامبی
     */
    public void hypnotizeZombie(GameSession session, Zombie zombie) { }

    /**
     * وقتی زامبی می‌میرد اتفاق می‌افتد:
     * drop سکه/الماس/گلدان، plant food از glowing zombie.
     * @param session session جاری
     * @param zombie زامبی کشته‌شده
     */
    public void onZombieDeath(GameSession session, Zombie zombie) { }

    /**
     * وقتی گیاه از بین می‌رود اتفاق می‌افتد.
     * @param session session جاری
     * @param plant گیاه نابودشده
     */
    public void onPlantDestroyed(GameSession session, Plant plant) { }

    /**
     * رفتار ماشین چمن‌زنی را پردازش می‌کند.
     * @param session session جاری
     * @param row ردیف
     * @param zombie زامبی رسیده به انتها
     */
    public void handleLawnMower(GameSession session, int row, Zombie zombie) { }

    /**
     * زمین لیز را برای زامبی پردازش می‌کند.
     * @param session session جاری
     * @param zombie زامبی
     * @param tileX ستون خانه لیز
     * @param tileY ردیف خانه لیز
     */
    public void processSlipperyTile(GameSession session, Zombie zombie,
                                    int tileX, int tileY) { }
}
