package controller;

import model.AppState;
import service.ScoreService;
import service.UserService;
import view.ConsoleView;

/**
 * کنترلر جدول امتیازات.
 * نمایش و مرتب‌سازی لیدربورد.
 */
public class LeaderboardController {

    private final ScoreService scoreService;
    private final UserService userService;
    private final AppState appState;
    private final ConsoleView view;

    /** فیلد مرتب‌سازی فعلی */
    private String currentSortField;

    /** آیا مرتب‌سازی صعودی است */
    private boolean ascending;

    public LeaderboardController(ScoreService scoreService, UserService userService,
                                 AppState appState, ConsoleView view) {
        this.scoreService = scoreService;
        this.userService = userService;
        this.appState = appState;
        this.view = view;
    }

    /** لیدربورد را با مرتب‌سازی پیش‌فرض نمایش می‌دهد */
    public void showLeaderboard() { }

    /**
     * لیدربورد را با مرتب‌سازی مشخص نمایش می‌دهد.
     * @param sortBy فیلد (lastLevel, minigames, dailyQuests, quests, meoPoint)
     * @param ascending آیا صعودی
     */
    public void showLeaderboard(String sortBy, boolean ascending) { }
}
