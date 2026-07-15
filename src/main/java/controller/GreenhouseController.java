package controller;

import model.AppState;
import model.Greenhouse;
import model.Pot;
import model.User;
import service.GreenhouseService;
import view.ConsoleView;

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
        view.printHeader("🏡 Greenhouse (4×5)");
        printGreenhouseGrid(gh);
        System.out.println(ConsoleView.YELLOW
                + "  Coins: " + user.getCoins()
                + "  Gems: " + user.getGems() + ConsoleView.RESET);
    }

    private void printGreenhouseGrid(Greenhouse gh) {
        System.out.println(ConsoleView.CYAN
                + "     1          2          3          4          5"
                + ConsoleView.RESET);
        for (int row = 1; row <= 4; row++) {
            System.out.print(ConsoleView.CYAN + " " + row + " " + ConsoleView.RESET);
            for (int col = 1; col <= 5; col++) {
                Pot pot = gh.getPot(col, row);
                System.out.print(formatPot(pot) + " ");
            }
            System.out.println();
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
        System.out.println(ConsoleView.CYAN + "  ID            Item"
                + "                         Price" + ConsoleView.RESET);
        System.out.println("  POT           Unlock a greenhouse pot       "
                + ConsoleView.YELLOW + "2000 coins" + ConsoleView.RESET);
        System.out.println("  PLANT_FOOD    Plant food (start of level)   "
                + ConsoleView.MAGENTA + "3 gems" + ConsoleView.RESET);
        System.out.println("  SEED_RANDOM   5 random seed packets         "
                + ConsoleView.YELLOW + "1000 coins" + ConsoleView.RESET);
        System.out.println("  SEED_CHOICE   10 seed packets (pick plant)  "
                + ConsoleView.MAGENTA + "5 gems" + ConsoleView.RESET);
        System.out.println("  CURRENCY      500 coins                     "
                + ConsoleView.MAGENTA + "5 gems" + ConsoleView.RESET);
        view.printInfo("Usage: shop buy -i <ID> -n <count> [-t <plant>]");
    }

    public void showDailyOffer(AppState appState) {
        User user = appState.getCurrentUser();
        String today = java.time.LocalDate.now().toString();
        boolean purchased = today.equals(user.getLastDailyOfferDate());
        view.printHeader("🎁 Daily Offer");
        System.out.println(ConsoleView.YELLOW
                + "  10 Random Seed Packets" + ConsoleView.RESET);
        System.out.println(ConsoleView.GREEN
                + "  Price: 1600 coins (20% off!)" + ConsoleView.RESET);
        if (purchased) {
            System.out.println(ConsoleView.RED
                    + "  ✘ Already purchased today." + ConsoleView.RESET);
        } else {
            System.out.println(ConsoleView.GREEN
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
