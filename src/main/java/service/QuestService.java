package service;

import model.Quest;
import model.User;
import model.enums.QuestType;

import java.util.List;

/**
 * سرویس مدیریت کوئست‌ها.
 * ردیابی پیشرفت، تکمیل و پاداش کوئست‌ها.
 */
public class QuestService {

    private final UserService userService;

    public QuestService(UserService userService) {
        this.userService = userService;
    }

    /**
     * لیست کوئست‌های یک صفحه از Travel Log را برمی‌گرداند.
     * با اولویت‌بندی: critical > high > medium > low.
     * @param user کاربر
     * @param questType نوع صفحه
     * @return لیست کوئست‌ها
     */
    public List<Quest> getQuestsByType(User user, QuestType questType) { return null; }

    /**
     * پیشرفت کوئست‌های مربوط به کشتن زامبی را به‌روز می‌کند.
     * @param user کاربر
     * @param count تعداد زامبی کشته‌شده
     */
    public void onZombiesKilled(User user, int count) { }

    /**
     * پیشرفت کوئست‌های مربوط به تکمیل مرحله را به‌روز می‌کند.
     * @param user کاربر
     * @param levelNumber شماره مرحله
     */
    public void onLevelCompleted(User user, int levelNumber) { }

    /**
     * پیشرفت کوئست‌های مربوط به تولید خورشید را به‌روز می‌کند.
     * @param user کاربر
     * @param amount مقدار خورشید
     */
    public void onSunProduced(User user, int amount) { }

    /**
     * پاداش کوئست تکمیل‌شده را به کاربر می‌دهد.
     * @param user کاربر
     * @param quest کوئست
     */
    public void claimReward(User user, Quest quest) { }

    /**
     * کوئست‌های روزانه را ریست می‌کند (اگر روز جدیدی است).
     * @param user کاربر
     */
    public void resetDailyQuestsIfNeeded(User user) { }

    /**
     * تمام کوئست‌های بازی را می‌سازد و برمی‌گرداند.
     * @return لیست تمام کوئست‌ها
     */
    public List<Quest> buildAllQuests() { return null; }
}
