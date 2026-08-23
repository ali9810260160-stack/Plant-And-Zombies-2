package com.pvz2.controller;

import com.pvz2.model.AppState;
import com.pvz2.model.Greenhouse;
import com.pvz2.model.Pot;
import com.pvz2.model.User;
import com.pvz2.service.GreenhouseService;
import com.pvz2.view.ConsoleView;

import java.util.regex.Matcher;

/**
 * کنترلر گلخانه و فروشگاه.
 */
public class GreenhouseController {

    private final GreenhouseService greenhouseService;
    private final ConsoleView view;

    public GreenhouseController(GreenhouseService greenhouseService,
                                ConsoleView view) {
        this.greenhouseService = greenhouseService;
        this.view = view;
    }

    public void showGreenhouse(AppState appState) {
        User user = appState.getCurrentUser();
        Greenhouse gh = greenhouseService.getOrCreateGreenhouse(user);
        view.printHeader("🏡 Greenhouse (" + Greenhouse.ROWS + "×" + Greenhouse.COLS + ")");
        printGreenhouseGrid(gh);
        view.printRaw(ConsoleView.YELLOW
                + "  Coins: " + user.getCoins()
                + "  Gems: " + user.getGems() + ConsoleView.RESET);
    }

    private void printGreenhouseGrid(Greenhouse gh) {
        StringBuilder header = new StringBuilder("    ");
        for (int col = 1; col <= Greenhouse.COLS; col++) {
            header.append(String.format("%-11d", col));
        }
        view.printRaw(ConsoleView.CYAN + header + ConsoleView.RESET);
        for (int row = 1; row <= Greenhouse.ROWS; row++) {
            java.util.List<String> cells = new java.util.ArrayList<>();
            for (int col = 1; col <= Greenhouse.COLS; col++) {
                cells.add(formatPot(gh.getPot(col, row)));
            }
            view.getGreenhouseView().printGreenhouseRow(row, cells);
        }
    }

    private String formatPot(Pot pot) {
        if (pot == null) {
            return ConsoleView.RED + "[  NONE  ]" + ConsoleView.RESET;
        }
        if (pot.isLocked()) {
            return ConsoleView.RED + "[ LOCKED ]" + ConsoleView.RESET;
        }
        if (pot.getPlantType() == null) {
            return ConsoleView.WHITE + "[  EMPTY ]" + ConsoleView.RESET;
        }
        if (pot.isReady()) {
            return ConsoleView.GREEN + ConsoleView.BOLD
                    + "[  READY ]" + ConsoleView.RESET;
        }
        String name = pot.getPlantType().length() > 7
                ? pot.getPlantType().substring(0, 7) : pot.getPlantType();
        return ConsoleView.YELLOW + "[" + String.format("%-8s", name)
                + "]" + ConsoleView.RESET;
    }

    public void plantPot(Matcher m, AppState appState) {
        User user = appState.getCurrentUser();
        int x = Integer.parseInt(m.group(1));
        int y = Integer.parseInt(m.group(2));
        greenhouseService.plantPot(user, x, y);
        view.printSuccess("Plant seeded at (" + x + ", " + y + ")!");
    }

    public void collectPot(Matcher m, AppState appState) {
        User user = appState.getCurrentUser();
        int x = Integer.parseInt(m.group(1));
        int y = Integer.parseInt(m.group(2));
        greenhouseService.collectPot(user, x, y);
    }

    public void growPot(Matcher m, AppState appState) {
        User user = appState.getCurrentUser();
        int x = Integer.parseInt(m.group(1));
        int y = Integer.parseInt(m.group(2));
        greenhouseService.growPot(user, x, y);
    }

    public void showShopList() {
        view.printHeader("🛒 Shop — Permanent Items");
        view.printRaw(ConsoleView.CYAN + "  ID            Item"
                + "                         Price" + ConsoleView.RESET);
        view.printRaw("  POT           Unlock a greenhouse pot       "
                + ConsoleView.YELLOW + "2000 coins" + ConsoleView.RESET);
        view.printRaw("  PLANT_FOOD    Plant food (start of level)   "
                + ConsoleView.MAGENTA + "3 gems" + ConsoleView.RESET);
        view.printRaw("  SEED_RANDOM   5 random seed packets         "
                + ConsoleView.YELLOW + "1000 coins" + ConsoleView.RESET);
        view.printRaw("  SEED_CHOICE   10 seed packets (pick plant)  "
                + ConsoleView.MAGENTA + "5 gems" + ConsoleView.RESET);
        view.printRaw("  CURRENCY      500 coins                     "
                + ConsoleView.MAGENTA + "5 gems" + ConsoleView.RESET);
        view.printInfo("Usage: shop buy -i <ID> -n <count> [-t <plant>]");
    }

    public void showDailyOffer(AppState appState) {
        User user = appState.getCurrentUser();
        String today = java.time.LocalDate.now().toString();
        boolean purchased = today.equals(user.getLastDailyOfferDate());
        view.printHeader("🎁 Daily Offer");
        view.printRaw(ConsoleView.YELLOW
                + "  10 Random Seed Packets" + ConsoleView.RESET);
        view.printRaw(ConsoleView.GREEN
                + "  Price: 1600 coins (20% off!)" + ConsoleView.RESET);
        if (purchased) {
            view.printRaw(ConsoleView.RED
                    + "  ✘ Already purchased today." + ConsoleView.RESET);
        } else {
            view.printRaw(ConsoleView.GREEN
                    + "  ✔ Available! Use: shop buy -i DAILY -n 1"
                    + ConsoleView.RESET);
        }
    }

    public void shopBuy(Matcher m, AppState appState) {
        User user = appState.getCurrentUser();
        String itemId = m.group(1);
        int count = Integer.parseInt(m.group(2));
        String plantType = m.groupCount() >= 3 ? m.group(3) : null;
        greenhouseService.shopBuy(user, itemId, count, plantType);
    }
}
