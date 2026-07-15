package service;

import exception.GameException;
import model.AppState;
import model.Greenhouse;
import model.Pot;
import model.User;
import model.enums.PlantType;
import repository.UserRepository;
import util.RandomUtil;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * سرویس گلخانه و فروشگاه.
 */
public class GreenhouseService {

    private final UserService userService;
    private final UserRepository userRepository;
    private static final int MAX_POTS = 20;
    private static final int MARIGOLD_GROW_HOURS = 2;
    private static final int PLANT_GROW_HOURS = 8;
    private static final long MARIGOLD_REWARD_COINS = 500;

    public GreenhouseService(UserService userService,
                             UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    public Greenhouse getOrCreateGreenhouse(User user) {
        if (user.getGreenhouse() == null) {
            Greenhouse gh = new Greenhouse();
            user.setGreenhouse(gh);
        }
        return user.getGreenhouse();
    }

    public void plantPot(User user, int x, int y) {
        Greenhouse gh = getOrCreateGreenhouse(user);
        Pot pot = gh.getPot(x, y);
        if (pot == null) {
            throw new GameException(
                    "No pot exists at (" + x + ", " + y + ").");
        }
        if (pot.isLocked()) {
            throw new GameException(
                    "Pot at (" + x + ", " + y + ") is locked. Buy it first.");
        }
        if (pot.getPlantType() != null) {
            throw new GameException(
                    "Pot at (" + x + ", " + y + ") is already occupied.");
        }
        boolean isMarigold = RandomUtil.chance(0.5);
        if (isMarigold) {
            pot.setPlantType("MARIGOLD");
            pot.setGrowthHours(MARIGOLD_GROW_HOURS);
        } else {
            String randomPlant = pickRandomUnlockedPlant(user);
            pot.setPlantType(randomPlant);
            pot.setGrowthHours(PLANT_GROW_HOURS);
        }
        pot.setPlantedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private String pickRandomUnlockedPlant(User user) {
        List<String> unlocked = user.getUnlockedPlants();
        if (unlocked == null || unlocked.isEmpty()) {
            return "MARIGOLD";
        }
        List<String> plantFoodPlants = getPlantFoodCapablePlants(unlocked);
        if (plantFoodPlants.isEmpty()) {
            return "MARIGOLD";
        }
        return plantFoodPlants.get(RandomUtil.nextInt(plantFoodPlants.size()));
    }

    private List<String> getPlantFoodCapablePlants(List<String> unlocked) {
        List<String> result = new ArrayList<>();
        for (String p : unlocked) {
            if (!p.equals("MARIGOLD")) {
                result.add(p);
            }
        }
        return result;
    }

    public void collectPot(User user, int x, int y) {
        Greenhouse gh = getOrCreateGreenhouse(user);
        Pot pot = gh.getPot(x, y);
        if (pot == null || pot.isLocked()) {
            throw new GameException("No accessible pot at (" + x + ", " + y + ").");
        }
        if (pot.getPlantType() == null) {
            throw new GameException("No plant in pot at (" + x + ", " + y + ").");
        }
        if (!pot.isReady()) {
            double hoursLeft = getRemainingHours(pot);
            throw new GameException(
                    "Plant not ready yet. " + String.format("%.1f", hoursLeft)
                            + " hours remaining.");
        }
        if (pot.getPlantType().equals("MARIGOLD")) {
            userService.addCoins(user, MARIGOLD_REWARD_COINS);
            System.out.println("\u001B[32m🌸 Harvested marigold: +"
                    + MARIGOLD_REWARD_COINS + " coins!\u001B[0m");
        } else {
            giveStoredBoost(user, pot.getPlantType());
            System.out.println("\u001B[32m🌱 Harvested "
                    + pot.getPlantType()
                    + " - stored boost ready for next use!\u001B[0m");
        }
        pot.setPlantType(null);
        pot.setPlantedAt(null);
        userRepository.save(user);
    }

    private void giveStoredBoost(User user, String plantType) {
        System.out.println("\u001B[35mStored boost for "
                + plantType + " activated!\u001B[0m");
    }

    public void growPot(User user, int x, int y) {
        Greenhouse gh = getOrCreateGreenhouse(user);
        Pot pot = gh.getPot(x, y);
        if (pot == null || pot.isLocked() || pot.getPlantType() == null) {
            throw new GameException("No plant to accelerate at (" + x + ", " + y + ").");
        }
        if (pot.isReady()) {
            throw new GameException("Plant is already ready to harvest!");
        }
        double hoursLeft = getRemainingHours(pot);
        int gemsNeeded = (int) Math.ceil(hoursLeft);
        if (user.getGems() < gemsNeeded) {
            throw new GameException(
                    "Need " + gemsNeeded + " gems, you have " + user.getGems() + ".");
        }
        userService.spendGems(user, gemsNeeded);
        pot.setPlantedAt(LocalDateTime.now()
                .minus(pot.getGrowthHours(), ChronoUnit.HOURS));
        userRepository.save(user);
        System.out.println("\u001B[32m⚡ Growth accelerated! Plant is now ready.\u001B[0m");
    }

    private double getRemainingHours(Pot pot) {
        if (pot.getPlantedAt() == null) {
            return pot.getGrowthHours();
        }
        long minutesElapsed = ChronoUnit.MINUTES.between(
                pot.getPlantedAt(), LocalDateTime.now());
        double hoursElapsed = minutesElapsed / 60.0;
        return Math.max(0, pot.getGrowthHours() - hoursElapsed);
    }

    public void buyPot(User user, int x, int y) {
        Greenhouse gh = getOrCreateGreenhouse(user);
        Pot pot = gh.getPot(x, y);
        if (pot == null) {
            throw new GameException("Invalid pot position (" + x + ", " + y + ").");
        }
        if (!pot.isLocked()) {
            throw new GameException("Pot at (" + x + ", " + y + ") is already unlocked.");
        }
        if (gh.getUnlockedCount() >= MAX_POTS) {
            throw new GameException("Maximum pots reached (20).");
        }
        if (!userService.spendCoins(user, 2000)) {
            throw new GameException("Not enough coins. Need 2000.");
        }
        pot.setLocked(false);
        userRepository.save(user);
        System.out.println("\u001B[32m🏺 Pot unlocked at ("
                + x + ", " + y + ")!\u001B[0m");
    }

    public void shopBuy(User user, String itemId, int count, String plantType) {
        switch (itemId.toUpperCase()) {
            case "POT": buyPotItem(user, count); break;
            case "PLANT_FOOD": buyPlantFood(user, count); break;
            case "SEED_RANDOM": buySeedPacketsRandom(user, count); break;
            case "SEED_CHOICE": buySeedPacketsChoice(user, count, plantType); break;
            case "CURRENCY": convertCurrency(user, count); break;
            case "DAILY": buyDailyOffer(user); break;
            default:
                throw new GameException("Unknown item: " + itemId);
        }
    }

    private void buyPotItem(User user, int count) {
        long cost = 2000L * count;
        if (!userService.spendCoins(user, cost)) {
            throw new GameException("Not enough coins (need " + cost + ").");
        }
        user.setPots(user.getPots() + count);
        userRepository.save(user);
        System.out.println("\u001B[32m🏺 Purchased " + count + " pot(s)!\u001B[0m");
    }

    private void buyPlantFood(User user, int count) {
        int cost = 3 * count;
        int newTotal = user.getPlantFoodCount() + count;
        if (newTotal > 3) {
            throw new GameException(
                    "Plant food capacity exceeded. Max 3, you have "
                            + user.getPlantFoodCount() + ".");
        }
        if (!userService.spendGems(user, cost)) {
            throw new GameException("Need " + cost + " gems.");
        }
        user.setPlantFoodCount(newTotal);
        userRepository.save(user);
        System.out.println("\u001B[32m✨ Purchased " + count
                + " plant food(s)!\u001B[0m");
    }

    private void buySeedPacketsRandom(User user, int count) {
        long cost = 1000L * (count / 5);
        if (!userService.spendCoins(user, cost)) {
            throw new GameException("Need " + cost + " coins.");
        }
        System.out.println("\u001B[32m🎁 Purchased " + count
                + " random seed packets!\u001B[0m");
    }

    private void buySeedPacketsChoice(User user, int count, String plantType) {
        if (plantType == null || plantType.isEmpty()) {
            throw new GameException("Plant type required for this item.");
        }
        int gemCost = 5 * (count / 10);
        if (!userService.spendGems(user, gemCost)) {
            throw new GameException("Need " + gemCost + " gems.");
        }
        System.out.println("\u001B[32m🎁 Purchased " + count
                + " " + plantType + " seed packets!\u001B[0m");
    }

    private void convertCurrency(User user, int count) {
        int gemCost = 5 * count;
        if (!userService.spendGems(user, gemCost)) {
            throw new GameException("Need " + gemCost + " gems.");
        }
        userService.addCoins(user, 500L * count);
        System.out.println("\u001B[32m💱 Converted " + gemCost
                + " gems to " + (500 * count) + " coins!\u001B[0m");
    }

    private void buyDailyOffer(User user) {
        String today = java.time.LocalDate.now().toString();
        if (today.equals(user.getLastDailyOfferDate())) {
            throw new GameException("Daily offer already purchased today.");
        }
        if (!userService.spendCoins(user, 1600)) {
            throw new GameException("Need 1600 coins for daily offer.");
        }
        user.setLastDailyOfferDate(today);
        userRepository.save(user);
        System.out.println("\u001B[32m🎁 Daily offer purchased! 10 seed packets added.\u001B[0m");
    }
}
