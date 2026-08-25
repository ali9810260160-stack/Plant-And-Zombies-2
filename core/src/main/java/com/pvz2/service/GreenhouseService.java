package com.pvz2.service;

import com.pvz2.exception.GameException;
import com.pvz2.model.Greenhouse;
import com.pvz2.model.Pot;
import com.pvz2.model.User;
import com.pvz2.repository.UserRepository;
import com.pvz2.util.RandomUtil;
import com.pvz2.view.ConsoleView;

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
    private final ConsoleView view;
    private static final int MAX_POTS = 12;
    private static final int MARIGOLD_GROW_HOURS = 2;
    private static final int PLANT_GROW_HOURS = 8;
    private static final long MARIGOLD_REWARD_COINS = 500;
    /** جایزه‌ی سکه هنگام برداشتِ گیاهِ غیرِ marigold وقتی plant food پُر است. */
    private static final long GREENHOUSE_HARVEST_COINS = 250;
    /** هزینه‌ی الماس برای کاشتن یک گیاه در جایگاه خالی (نشان داده‌شده روی diamond-ticket در UI). */
    private static final int PLANT_COST_GEMS = 20;
    /** هزینه‌ی سکه برای باز کردن یک جایگاه قفل. */
    private static final long UNLOCK_POT_COST_COINS = 2000;

    public GreenhouseService(UserService userService,
                             UserRepository userRepository, ConsoleView view) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.view = view;
    }

    public Greenhouse getOrCreateGreenhouse(User user) {
        if (user.getGreenhouse() == null) {
            Greenhouse gh = new Greenhouse();
            user.setGreenhouse(gh);
        }
        return user.getGreenhouse();
    }

    /** هزینه‌ی الماس کاشت — UI به‌جای هاردکد کردن عدد روی تیکت، از این می‌خونه. */
    public int getPlantCostGems() {
        return PLANT_COST_GEMS;
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
        if (!userService.spendGems(user, PLANT_COST_GEMS)) {
            throw new GameException(
                    "Need " + PLANT_COST_GEMS + " gems to plant.");
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

    public String collectPot(User user, int x, int y) {
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
            return ("\u001B[32m🌸 Harvested marigold: +"
                    + MARIGOLD_REWARD_COINS + " coins!\u001B[0m");
        } else {
            String harvestedPlant = pot.getPlantType();
            // جایزه واقعاً به user اضافه می‌شود (قبلاً giveStoredBoost فقط یک پیام
            // کنسولی چاپ می‌کرد و چیزی به کاربر افزوده نمی‌شد): boost دائمی این
            // گیاه + یک plant food (تا سقف ۳) وگرنه سکه.
            user.setPlantBoosted(harvestedPlant, true);
            String bonus;
            if (user.getPlantFoodCount() < 3) {
                user.setPlantFoodCount(user.getPlantFoodCount() + 1);
                bonus = "+1 Plant Food";
            } else {
                userService.addCoins(user, GREENHOUSE_HARVEST_COINS);
                bonus = "+" + GREENHOUSE_HARVEST_COINS + " coins";
            }
            pot.setPlantType(null);
            pot.setPlantedAt(null);
            userRepository.save(user);
            return ("\u001B[32m🌱 Harvested "
                    + harvestedPlant
                    + " — stored boost & " + bonus + "!\u001B[0m");
        }
    }

    private void giveStoredBoost(User user, String plantType) {
        view.printRaw("\u001B[35mStored boost for "
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
        int gemsNeeded = getSpeedupCostGems(pot);
        if (user.getGems() < gemsNeeded) {
            throw new GameException(
                    "Need " + gemsNeeded + " gems, you have " + user.getGems() + ".");
        }
        userService.spendGems(user, gemsNeeded);
        pot.setPlantedAt(LocalDateTime.now()
                .minus(pot.getGrowthHours(), ChronoUnit.HOURS));
        userRepository.save(user);
        view.printRaw("\u001B[32m⚡ Growth accelerated! Plant is now ready.\u001B[0m");
    }

    /**
     * هزینه‌ی الماس برای تسریع رشد یک گلدان — بدون هیچ تغییری در وضعیت
     * (فقط محاسبه، برای نمایش عدد روی دکمه‌ی تسریع در UI هم استفاده می‌شه).
     */
    public int getSpeedupCostGems(Pot pot) {
        if (pot == null || pot.getPlantType() == null) return 0;
        double hoursLeft = getRemainingHours(pot);
        return (int) Math.ceil(Math.max(0, hoursLeft));
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
            throw new GameException("Maximum pots reached (" + MAX_POTS + ").");
        }
        if (!userService.spendCoins(user, UNLOCK_POT_COST_COINS)) {
            throw new GameException("Not enough coins. Need " + UNLOCK_POT_COST_COINS + ".");
        }
        pot.setLocked(false);
        userRepository.save(user);
        view.printRaw("\u001B[32m🏺 Pot unlocked at ("
                + x + ", " + y + ")!\u001B[0m");
    }

    /** هزینه‌ی سکه برای باز کردن یک جایگاه قفل — UI به‌جای هاردکد از این می‌خونه. */
    public long getUnlockCostCoins() {
        return UNLOCK_POT_COST_COINS;
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
        view.printRaw("\u001B[32m🏺 Purchased " + count + " pot(s)!\u001B[0m");
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
        view.printRaw("\u001B[32m✨ Purchased " + count
                + " plant food(s)!\u001B[0m");
    }

    private void buySeedPacketsRandom(User user, int count) {
        long cost = 1000L * (count / 5);
        if (!userService.spendCoins(user, cost)) {
            throw new GameException("Need " + cost + " coins.");
        }
        // بسته‌بذر تصادفی — توزیع بین گیاهان آنلاک‌شده
        java.util.List<String> owned = user.getUnlockedPlants();
        if (owned != null && !owned.isEmpty()) {
            java.util.Random rng = new java.util.Random();
            for (int i = 0; i < count; i++) {
                String p = owned.get(rng.nextInt(owned.size()));
                user.addSeedPackets(p, 1);
            }
        }
        userService.save(user);
        view.printRaw("\u001B[32m🎁 Purchased " + count
                + " random seed packets (distributed among your plants)!\u001B[0m");
    }

    private void buySeedPacketsChoice(User user, int count, String plantType) {
        if (plantType == null || plantType.isEmpty()) {
            throw new GameException("Plant type required for this item.");
        }
        int gemCost = 5 * (count / 10);
        if (!userService.spendGems(user, gemCost)) {
            throw new GameException("Need " + gemCost + " gems.");
        }
        // ذخیره بسته‌بذر برای گیاه انتخابی
        user.addSeedPackets(plantType.toUpperCase(), count);
        userService.save(user);
        view.printRaw("\u001B[32m🎁 Purchased " + count
                + " " + plantType + " seed packets! (Total: "
                + user.getSeedPackets(plantType.toUpperCase()) + ")\u001B[0m");
    }

    private void convertCurrency(User user, int count) {
        int gemCost = 5 * count;
        if (!userService.spendGems(user, gemCost)) {
            throw new GameException("Need " + gemCost + " gems.");
        }
        userService.addCoins(user, 500L * count);
        view.printRaw("\u001B[32m💱 Converted " + gemCost
                + " gems to " + (500 * count) + " coins!\u001B[0m");
    }

    private void buyDailyOffer(User user) {
        ensureDailyOfferFresh(user);
        String today = java.time.LocalDate.now().toString();
        if (today.equals(user.getLastDailyOfferDate())) {
            throw new GameException("Daily offer already purchased today.");
        }
        if (!userService.spendCoins(user, 1600)) {
            throw new GameException("Need 1600 coins for daily offer.");
        }
        // باگ قبلی: این متد هیچ بسته‌بذری واقعاً اضافه نمی‌کرد و مشخص نمی‌کرد
        // پیشنهاد امروز برای کدام گیاه است — ۱۶۰۰ سکه کم می‌شد بدون هیچ جایزه‌ای.
        String plant = user.getDailyOfferPlant();
        user.addSeedPackets(plant, 10);
        user.setLastDailyOfferDate(today);
        userRepository.save(user);
        view.printRaw("\u001B[32m🎁 Daily offer purchased! 10 " + plant
                + " seed packets added.\u001B[0m");
    }

    /**
     * پیشنهاد روزانه امروز را (در صورت نیاز) تولید و ذخیره می‌کند — یک گیاه
     * تصادفی از بین گیاهان آنلاک‌شده کاربر، فقط یک‌بار در روز عوض می‌شود.
     */
    private void ensureDailyOfferFresh(User user) {
        String today = java.time.LocalDate.now().toString();
        if (!today.equals(user.getDailyOfferGeneratedDate())) {
            user.setDailyOfferPlant(pickRandomUnlockedPlant(user));
            user.setDailyOfferGeneratedDate(today);
            userRepository.save(user);
        }
    }

    /** نام گیاه پیشنهاد روزانه امروز — برای نمایش در فروشگاه. */
    public String getDailyOfferPlant(User user) {
        ensureDailyOfferFresh(user);
        return user.getDailyOfferPlant();
    }

    /** آیا پیشنهاد روزانه امروز قبلاً خریداری شده؟ */
    public boolean isDailyOfferPurchased(User user) {
        String today = java.time.LocalDate.now().toString();
        return today.equals(user.getLastDailyOfferDate());
    }
}
