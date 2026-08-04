package com.pvz2.controller;

import com.pvz2.model.AppState;
import com.pvz2.model.NewsItem;
import com.pvz2.model.User;
import com.pvz2.view.ConsoleView;

import java.util.List;

/**
 * کنترلر منوی اخبار.
 */
public class NewsController {

    private final ConsoleView view;

    public NewsController(ConsoleView view) {
        this.view = view;
    }

    public void showUnread(AppState appState) {
        User user = appState.getCurrentUser();
        List<NewsItem> unread = user.getUnreadNews();
        if (unread == null || unread.isEmpty()) {
            view.printInfo("No unread news.");
            return;
        }
        view.printHeader("📰 Unread News");
        for (NewsItem item : unread) {
            view.printRaw(ConsoleView.YELLOW + "  [" + item.getDate()
                + "] " + ConsoleView.RESET + item.getMessage());
        }
        unread.clear();
        view.printInfo("All news marked as read.");
    }

    public void showAll(AppState appState) {
        User user = appState.getCurrentUser();
        List<NewsItem> all = user.getAllNews();
        if (all == null || all.isEmpty()) {
            view.printInfo("No news yet.");
            return;
        }
        view.printHeader("📰 All News");
        for (NewsItem item : all) {
            String readMark = item.isRead()
                ? ConsoleView.GREEN + "[read]   " + ConsoleView.RESET
                : ConsoleView.RED + "[unread] " + ConsoleView.RESET;
            view.printRaw("  " + readMark
                + ConsoleView.YELLOW + "[" + item.getDate() + "] "
                + ConsoleView.RESET + item.getMessage());
        }
    }
}
