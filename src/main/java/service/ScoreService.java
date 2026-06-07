package service;

import model.GameSession;
import model.User;
import model.zombies.Zombie;

import java.util.List;

/**
 * سرویس محاسبه میوپوینت و لیدربورد.
 * 5 الگوی امتیازگیری تعریف‌شده در بازی امتیازی.
 */
public class ScoreService {

    /**
     * وقتی چند زامبی با یک تیر کشته می‌شوند امتیاز اضافه می‌کند.
     * الگو 1: کشتن چند زامبی با یک تیر.
     * @param session session جاری
     * @param count تعداد زامبی‌های کشته‌شده
     */
    public void onMultiKillWithProjectile(GameSession session, int count) { }

    /**
     * وقتی زامبی سریع کشته می‌شود امتیاز اضافه می‌کند.
     * الگو 2: کشتن سریع زامبی (در کمتر از 3 ثانیه از spawn).
     * @param session session جاری
     * @param zombie زامبی کشته‌شده
     */
    public void onQuickKill(GameSession session, Zombie zombie) { }

    /**
     * وقتی چند زامبی همزمان می‌میرند امتیاز اضافه می‌کند.
     * الگو 3: کشتن همزمان زامبی‌ها (AoE).
     * @param session session جاری
     * @param count تعداد
     */
    public void onSimultaneousKill(GameSession session, int count) { }

    /**
     * وقتی موجی کامل شود بدون باخت دادن گیاه امتیاز می‌دهد.
     * الگو 4: تکمیل موج بدون از دست دادن گیاه.
     * @param session session جاری
     */
    public void onWaveCompletedNoDamage(GameSession session) { }

    /**
     * وقتی بازیکن سکه/الماس جمع‌آوری کند امتیاز می‌دهد.
     * الگو 5: جمع‌آوری آیتم‌های افتاده از زامبی.
     * @param session session جاری
     * @param isGem آیا الماس است
     */
    public void onItemCollected(GameSession session, boolean isGem) { }

    /**
     * لیدربورد را برمی‌گرداند.
     * @param users تمام کاربران
     * @param sortBy فیلد مرتب‌سازی
     * @param ascending آیا صعودی
     * @return لیست مرتب‌شده
     */
    public List<User> getLeaderboard(List<User> users, String sortBy,
                                     boolean ascending) { return null; }

    /**
     * میوپوینت نهایی بازی را به کاربر ثبت می‌کند.
     * @param user کاربر
     * @param session session تمام‌شده
     */
    public void finalizeScore(User user, GameSession session) { }
}
