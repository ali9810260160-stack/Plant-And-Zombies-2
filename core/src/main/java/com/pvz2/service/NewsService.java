package com.pvz2.service;

import com.pvz2.model.User;
import com.pvz2.repository.UserRepository;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * سرویس اخبار بازی — فاز ۱ و ۲.
 *
 * <p>اخبار هر کاربر در فایل {@code data/news/news_{username}.txt} ذخیره می‌شود.
 * هر خط یک خبر است با فرمت:
 * <pre>READ|DATE|TITLE|CONTENT</pre>
 *
 * <p>خبرها هنگام باز شدن گیاه/زامبی/مرحله/مینی‌گیم جدید به صورت خودکار اضافه می‌شوند.
 * تاریخ‌ها به فرمت {@code yyyy-MM-dd} ذخیره می‌شوند.
 */
public class NewsService {

    private static final String NEWS_DIR = "data/news/";
    private static final String SEPARATOR = "|";
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final UserRepository userRepository;

    public NewsService(UserRepository userRepository) {
        this.userRepository = userRepository;
        initDirectory();
    }

    private void initDirectory() {
        try { Files.createDirectories(Paths.get(NEWS_DIR)); }
        catch (IOException ignored) {}
    }

    // =========================================================================
    //  Read — خواندن اخبار
    // =========================================================================

    /** همه اخبار کاربر (از جدید به قدیم) */
    public List<NewsItem> getAllNews(User user) {
        List<NewsItem> items = loadFromFile(user);
        items.sort(Comparator.comparing((NewsItem n) -> n.date).reversed());
        return items;
    }

    /** فقط اخبار خوانده‌نشده */
    public List<NewsItem> getUnreadNews(User user) {
        List<NewsItem> items = getAllNews(user);
        List<NewsItem> unread = new ArrayList<>();
        for (NewsItem item : items) {
            if (!item.read) unread.add(item);
        }
        return unread;
    }

    /** تعداد اخبار خوانده‌نشده */
    public int getUnreadCount(User user) {
        return getUnreadNews(user).size();
    }

    /** علامت‌گذاری یک خبر به عنوان خوانده‌شده */
    public void markAsRead(User user, String newsId) {
        List<NewsItem> items = loadFromFile(user);
        for (NewsItem item : items) {
            if (item.id.equals(newsId)) { item.read = true; break; }
        }
        saveToFile(user, items);
    }

    /** علامت‌گذاری همه اخبار به عنوان خوانده‌شده */
    public void markAllAsRead(User user) {
        List<NewsItem> items = loadFromFile(user);
        for (NewsItem item : items) item.read = true;
        saveToFile(user, items);
    }

    // =========================================================================
    //  Write — اضافه کردن خبر
    // =========================================================================

    /** افزودن یک خبر دلخواه */
    public void addNews(User user, String title, String content) {
        List<NewsItem> items = loadFromFile(user);
        NewsItem item = new NewsItem();
        item.id      = UUID.randomUUID().toString().substring(0, 8);
        item.date    = LocalDate.now().format(DATE_FMT);
        item.title   = title;
        item.content = content;
        item.read    = false;
        items.add(0, item);
        saveToFile(user, items);
    }

    // =========================================================================
    //  Game Event Hooks — رویدادهای بازی
    // =========================================================================

    /** هنگام باز شدن گیاه جدید */
    public void onPlantUnlocked(User user, String plantName) {
        addNews(user,
            "New Plant Unlocked: " + formatName(plantName),
            "You have unlocked the " + formatName(plantName)
                + "! Head to your Collection to check it out.");
    }

    /** هنگام خریدنِ گیاهِ جدید (باز کردن با سکه). */
    public void onPlantBought(User user, String plantName) {
        addNews(user,
            "Plant Purchased: " + formatName(plantName),
            "You bought the " + formatName(plantName)
                + "! It is now available in your collection.");
    }

    /** هنگام ارتقای یک گیاه. */
    public void onPlantUpgraded(User user, String plantName, int newLevel) {
        addNews(user,
            "Plant Upgraded: " + formatName(plantName),
            "Your " + formatName(plantName)
                + " reached upgrade level " + newLevel + "!");
    }

    /** هنگام boost کردنِ یک گیاه (با الماس یا از گلخانه). */
    public void onPlantBoosted(User user, String plantName) {
        addNews(user,
            "Plant Boosted: " + formatName(plantName),
            "Your " + formatName(plantName)
                + " is now boosted and starts the next battle supercharged!");
    }

    /** هنگام اولین بار دیدن زامبی جدید */
    public void onZombieFirstSeen(User user, String zombieName) {
        addNews(user,
            "New Zombie Encountered: " + formatName(zombieName),
            "A " + formatName(zombieName)
                + " appeared in battle for the first time!"
                + " Check the Almanac to learn more.");
    }

    /** هنگام تکمیل یک مرحله */
    public void onLevelCompleted(User user, String chapterName, int levelNumber) {
        addNews(user,
            "Level Completed: " + formatName(chapterName) + " — Level " + levelNumber,
            "You successfully defended your garden in "
                + formatName(chapterName) + ", Level " + levelNumber
                + "! New levels may have been unlocked.");
    }

    /** هنگام باز شدن مرحله جدید */
    public void onLevelUnlocked(User user, String chapterName, int levelNumber) {
        addNews(user,
            "New Level Unlocked: " + formatName(chapterName) + " — Level " + levelNumber,
            "A new challenge awaits you in "
                + formatName(chapterName) + "! Level " + levelNumber
                + " is now available.");
    }

    /** هنگام باز شدن مینی‌گیم جدید */
    public void onMinigameUnlocked(User user, String minigameName) {
        addNews(user,
            "Minigame Unlocked: " + formatName(minigameName),
            "A new minigame '" + formatName(minigameName)
                + "' has been unlocked! Find it in the Travel Log.");
    }

    /** خبر سیستمی (مثلاً اتصال شبکه) */
    public void addSystemNews(User user, String title, String content) {
        addNews(user, "[System] " + title, content);
    }

    // =========================================================================
    //  Persistence
    // =========================================================================

    private List<NewsItem> loadFromFile(User user) {
        List<NewsItem> items = new ArrayList<>();
        Path path = Paths.get(NEWS_DIR + "news_" + user.getUsername() + ".txt");
        if (!Files.exists(path)) return items;

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                NewsItem item = parseLine(line);
                if (item != null) items.add(item);
            }
        } catch (IOException ignored) {}
        return items;
    }

    private void saveToFile(User user, List<NewsItem> items) {
        Path path = Paths.get(NEWS_DIR + "news_" + user.getUsername() + ".txt");
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            for (NewsItem item : items) {
                writer.write(encodeLine(item));
                writer.newLine();
            }
        } catch (IOException ignored) {}
    }

    private NewsItem parseLine(String line) {
        // فرمت: READ|DATE|ID|TITLE|CONTENT
        String[] parts = line.split("\\" + SEPARATOR, 5);
        if (parts.length < 5) return null;
        NewsItem item = new NewsItem();
        item.read    = "1".equals(parts[0]);
        item.date    = parts[1];
        item.id      = parts[2];
        item.title   = unescape(parts[3]);
        item.content = unescape(parts[4]);
        return item;
    }

    private String encodeLine(NewsItem item) {
        return (item.read ? "1" : "0") + SEPARATOR
                + item.date + SEPARATOR
                + item.id   + SEPARATOR
                + escape(item.title)   + SEPARATOR
                + escape(item.content);
    }

    /** escape برای جلوگیری از تداخل با جداکننده */
    private String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("|", "\\|").replace("\n", "\\n");
    }

    private String unescape(String s) {
        return s == null ? "" : s.replace("\\n", "\n").replace("\\|", "|").replace("\\\\", "\\");
    }

    private String formatName(String raw) {
        if (raw == null) return "";
        String s = raw.replace("_", " ").replace("-", " ");
        String[] words = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)));
                sb.append(w.substring(1).toLowerCase());
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    // =========================================================================
    //  Inner class
    // =========================================================================

    /** یک آیتم خبر */
    public static class NewsItem {
        /** شناسه یکتا */
        public String  id;
        /** تاریخ به فرمت yyyy-MM-dd */
        public String  date;
        /** عنوان (انگلیسی) */
        public String  title;
        /** متن خبر (انگلیسی) */
        public String  content;
        /** آیا خوانده شده؟ */
        public boolean read;

        /** متنی که در UI نمایش داده می‌شود */
        public String getDisplayDate() {
            return date != null ? date : "";
        }
    }
}
