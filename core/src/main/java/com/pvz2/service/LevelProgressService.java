package com.pvz2.service;

import com.pvz2.model.User;
import com.pvz2.model.enums.ChapterType;
import com.pvz2.repository.UserRepository;
import com.pvz2.view.ConsoleView;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * سرویس پیشرفت مرحله — باز کردن مراحل، سیو وضعیت، نمایش مراحل.
 */
public class LevelProgressService {

    /** تعداد مراحل هر فصل */
    private static final Map<ChapterType, Integer> LEVELS_PER_CHAPTER =
            new LinkedHashMap<>();

    /** ترتیب فصل‌ها */
    private static final ChapterType[] CHAPTER_ORDER = {
            ChapterType.ANCIENT_EGYPT,
            ChapterType.FROSTBITE_CAVES,
            ChapterType.BIG_WAVE_BEACH,
            ChapterType.DARK_AGES
    };

    static {
        LEVELS_PER_CHAPTER.put(ChapterType.ANCIENT_EGYPT,    4);
        LEVELS_PER_CHAPTER.put(ChapterType.FROSTBITE_CAVES,  4);
        LEVELS_PER_CHAPTER.put(ChapterType.BIG_WAVE_BEACH,   4);
        LEVELS_PER_CHAPTER.put(ChapterType.DARK_AGES,        4);
    }

    private final UserRepository userRepository;
    private final ConsoleView view;

    public LevelProgressService(UserRepository userRepository,
                                ConsoleView view) {
        this.userRepository = userRepository;
        this.view = view;
    }

    /** آیا یک مرحله برای بازیکن باز شده است */
    public boolean isLevelUnlocked(User user, ChapterType chapter,
                                   int levelNumber) {
        return user.isLevelUnlocked(chapter.name(), levelNumber);
    }

    /** باز کردن مرحله بعدی پس از موفقیت */
    public void onLevelCompleted(User user, ChapterType chapter,
                                 int levelNumber) {
        int totalLevels = LEVELS_PER_CHAPTER.getOrDefault(chapter, 4);
        updateLastReachedLevel(user, chapter, levelNumber);

        if (levelNumber < totalLevels) {
            user.unlockLevel(chapter.name(), levelNumber + 1);
            view.printRaw(ConsoleView.GREEN
                    + "  🔓 Level " + chapter.name() + " " + (levelNumber + 1)
                    + " unlocked!" + ConsoleView.RESET);
        } else {
            unlockNextChapter(user, chapter);
        }

        user.setLevelsCompleted(user.getLevelsCompleted() + 1);
        user.setGamesPlayed(user.getGamesPlayed() + 1);
        giveCompletionRewards(user, levelNumber);
        userRepository.save(user);
    }

    private void updateLastReachedLevel(User user, ChapterType chapter,
                                        int levelNumber) {
        String key = chapter.name() + "_" + levelNumber;
        String current = user.getLastReachedLevel();
        if (current == null || current.isEmpty()
                || compareProgress(key, current) > 0) {
            user.setLastReachedLevel(key);
        }
    }

    /** مقایسه پیشرفت: مثبت = key جدیدتر است */
    private int compareProgress(String key, String current) {
        try {
            String[] kParts = key.split("_(?=[0-9])");
            String[] cParts = current.split("_(?=[0-9])");
            int chapterCompare = kParts[0].compareTo(cParts[0]);
            if (chapterCompare != 0) {
                return chapterCompare;
            }
            int kLevel = Integer.parseInt(kParts[kParts.length - 1]);
            int cLevel = Integer.parseInt(cParts[cParts.length - 1]);
            return Integer.compare(kLevel, cLevel);
        } catch (Exception e) {
            return 1;
        }
    }

    private void unlockNextChapter(User user, ChapterType current) {
        ChapterType next = getNextChapter(current);
        if (next == null) {
            view.printRaw(ConsoleView.BOLD + ConsoleView.YELLOW
                    + "  🏆 Congratulations! You completed ALL chapters!"
                    + ConsoleView.RESET);
            return;
        }
        user.unlockLevel(next.name(), 1);
        view.printRaw(ConsoleView.GREEN + ConsoleView.BOLD
                + "  🔓 New chapter unlocked: " + next.name() + "!"
                + ConsoleView.RESET);
    }

    private ChapterType getNextChapter(ChapterType current) {
        for (int i = 0; i < CHAPTER_ORDER.length - 1; i++) {
            if (CHAPTER_ORDER[i] == current) {
                return CHAPTER_ORDER[i + 1];
            }
        }
        return null;
    }

    private void giveCompletionRewards(User user, int levelNumber) {
        long coinReward = 100L * levelNumber;
        user.setCoins(user.getCoins() + coinReward);
        view.printRaw(ConsoleView.YELLOW
                + "  💰 Level reward: +" + coinReward + " coins!"
                + ConsoleView.RESET);
        if (levelNumber == 4) {
            user.setGems(user.getGems() + 1);
            view.printRaw(ConsoleView.MAGENTA
                    + "  💎 Chapter completion bonus: +1 gem!"
                    + ConsoleView.RESET);
        }
    }

    /** نمایش همه مراحل یک فصل با وضعیت قفل/باز */
    public void showChapterLevels(User user, ChapterType chapter) {
        int total = LEVELS_PER_CHAPTER.getOrDefault(chapter, 4);
        view.printHeader("📋 Levels — " + chapter.name());
        for (int i = 1; i <= total; i++) {
            boolean unlocked = user.isLevelUnlocked(chapter.name(), i);
            String levelType = getLevelTypeName(chapter, i);
            String status;
            if (unlocked) {
                status = ConsoleView.GREEN + "✔ UNLOCKED" + ConsoleView.RESET;
            } else {
                status = ConsoleView.RED + "🔒 LOCKED  " + ConsoleView.RESET;
            }
            view.getLevelProgressView().printLevelRow(i, status, levelType);
        }
        view.printInfo("Play with: menu enter chapter "
                + chapter.name() + " -l <number>");
    }

    /** نمایش همه فصل‌ها */
    public void showAllChapters(User user) {
        view.printHeader("🗺 Adventure World Map");
        for (ChapterType chapter : CHAPTER_ORDER) {
            int total = LEVELS_PER_CHAPTER.getOrDefault(chapter, 4);
            long unlocked = countUnlocked(user, chapter);
            boolean chapterOpen = user.isLevelUnlocked(chapter.name(), 1);
            String status = chapterOpen
                    ? ConsoleView.GREEN + "OPEN" + ConsoleView.RESET
                    : ConsoleView.RED + "LOCKED" + ConsoleView.RESET;
            view.getLevelProgressView().printChapterRow(chapter.name(), status, unlocked, total);
        }
        view.printInfo("show levels -c <CHAPTER_NAME>  to see level details");
    }

    private long countUnlocked(User user, ChapterType chapter) {
        int total = LEVELS_PER_CHAPTER.getOrDefault(chapter, 4);
        int count = 0;
        for (int i = 1; i <= total; i++) {
            if (user.isLevelUnlocked(chapter.name(), i)) {
                count++;
            }
        }
        return count;
    }

    private String getLevelTypeName(ChapterType chapter, int level) {
        if (level == 4) {
            return "(BOSS)";
        }
        if (level == 1) {
            return "(Normal)";
        }
        switch (chapter) {
            case ANCIENT_EGYPT:
                return level == 2 ? "(Conveyor Belt)"
                        : "(Save Our Seeds)";
            case FROSTBITE_CAVES:
                return level == 2 ? "(Night Ops)"
                        : "(Timed War)";
            case BIG_WAVE_BEACH:
                return level == 2 ? "(Locked Plants)"
                        : "(Dead Line)";
            case DARK_AGES:
                return level == 2 ? "(Love Your Plants)"
                        : "(Plant What You Get)";
            default:
                return "(Special)";
        }
    }

    public int getLevelsInChapter(ChapterType chapter) {
        return LEVELS_PER_CHAPTER.getOrDefault(chapter, 4);
    }
}
