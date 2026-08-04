package com.pvz2.controller;

import com.pvz2.model.AppState;
import com.pvz2.model.enums.MenuType;
import com.pvz2.view.ConsoleView;

/**
 * کنترلر ناوبری بین منوها.
 */
public class MenuController {

    private final ConsoleView view;

    public MenuController(ConsoleView view) {
        this.view = view;
    }

    public void handleEnter(String menuName, AppState appState) {
        MenuType target = resolveMenu(menuName.toLowerCase());
        if (target == null) {
            view.printError("Unknown menu: " + menuName);
            return;
        }
        if (!isTransitionAllowed(appState.getCurrentMenu(), target)) {
            view.printError("Cannot go to " + menuName
                    + " from " + appState.getCurrentMenu().name());
            return;
        }
        appState.setCurrentMenu(target);
        view.printSuccess("Entered " + target.name() + " menu.");
    }

    public void handleExit(AppState appState) {
        MenuType current = appState.getCurrentMenu();
        MenuType target = getExitTarget(current);
        if (target == null) {
            view.printInfo("Exiting program...");
            System.exit(0);
        }
        appState.setCurrentMenu(target);
        view.printSuccess("Returned to " + target.name() + " menu.");
    }

    private MenuType resolveMenu(String name) {
        switch (name) {
            case "login":       return MenuType.LOGIN;
            case "register":    return MenuType.REGISTER;
            case "main":        return MenuType.MAIN;
            case "game":        return MenuType.GAME;
            case "settings":    return MenuType.SETTINGS;
            case "news":        return MenuType.NEWS;
            case "profile":     return MenuType.PROFILE;
            case "collection":  return MenuType.COLLECTION;
            case "greenhouse":  return MenuType.GREENHOUSE;
            case "leaderboard": return MenuType.LEADERBOARD;
            case "travel-log":  return MenuType.TRAVEL_LOG;
            case "shop":        return MenuType.SHOP;
            default:            return null;
        }
    }

    private boolean isTransitionAllowed(MenuType from, MenuType to) {
        switch (from) {
            case REGISTER:
                return to == MenuType.LOGIN;
            case LOGIN:
                return to == MenuType.REGISTER;
            case MAIN:
                return to == MenuType.GAME || to == MenuType.SETTINGS
                        || to == MenuType.NEWS || to == MenuType.PROFILE
                        || to == MenuType.GREENHOUSE || to == MenuType.LEADERBOARD
                        || to == MenuType.TRAVEL_LOG;
            case GAME:
                return to == MenuType.COLLECTION || to == MenuType.MAIN;
            case SETTINGS:
            case NEWS:
            case PROFILE:
            case GREENHOUSE:
            case LEADERBOARD:
            case TRAVEL_LOG:
                return to == MenuType.MAIN;
            case COLLECTION:
                return to == MenuType.GAME;
            case SHOP:
                return to == MenuType.GREENHOUSE;
            default:
                return false;
        }
    }

    private MenuType getExitTarget(MenuType current) {
        switch (current) {
            case REGISTER:      return null;
            case LOGIN:         return MenuType.REGISTER;
            case SETTINGS:
            case NEWS:
            case PROFILE:
            case GREENHOUSE:
            case LEADERBOARD:
            case TRAVEL_LOG:    return MenuType.MAIN;
            case GAME:          return MenuType.MAIN;
            case COLLECTION:    return MenuType.GAME;
            case SHOP:          return MenuType.GREENHOUSE;
            case PLANT_SELECT:  return MenuType.GAME;
            case IN_GAME:       return MenuType.GAME;
            default:            return MenuType.MAIN;
        }
    }
}
