package service;

import model.User;
import model.enums.PlantType;
import model.enums.ZombieType;
import model.plants.Plant;
import model.zombies.Zombie;
import repository.PlantDataRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * سرویس کلکسیون گیاهان و زامبی‌ها.
 * ارتقا، خرید و نمایش اطلاعات گیاهان.
 */
public class CollectionService {

    private final UserService userService;
    private final PlantDataRepository plantDataRepository;


    public CollectionService(UserService userService, PlantDataRepository plantDataRepository) {
        this.userService = userService;
        this.plantDataRepository = plantDataRepository;
    }

    public PlantDataRepository getPlantDataRepository() {
        return plantDataRepository;
    }

    /**
     * لیست گیاهان آنلاک‌شده کاربر را برمی‌گرداند.
     * @param user کاربر
     * @return لیست انواع گیاه
     */
    public List<PlantType> getUnlockedPlants(User user) {
        List<PlantType> unlockedPlants = new ArrayList<>();

        for(String plantName : user.getUnlockedPlants()){
            unlockedPlants.add(turnPlantNameToPlantType(plantName));
        }

        return unlockedPlants;
    }

    public PlantType turnPlantNameToPlantType(String plantName){
        String upperCaseName = plantName.toUpperCase();
        switch (upperCaseName){
            case "SUNFLOWER": return PlantType.SUNFLOWER;
            case "TWIN_SUNFLOWER": return PlantType.TWIN_SUNFLOWER;
            case "SUN_SHROOM": return PlantType.SUN_SHROOM;
            case "SUN_BEAN": return PlantType.SUN_BEAN;
            case "PEASHOOTER": return PlantType.PEASHOOTER;
            case "REPEATER": return PlantType.REPEATER;
            case "GATLING_PEA": return PlantType.GATLING_PEA;
            case "MEGA_GATLING_PEA": return PlantType.MEGA_GATLING_PEA;
            case "SNOW_PEA": return PlantType.SNOW_PEA;
            case "TORCHWOOD": return PlantType.TORCHWOOD;
            case "SPIKEWEED": return PlantType.SPIKEWEED;
            case "CABBAGE_PULT": return PlantType.CABBAGE_PULT;
            case "MELON_PULT": return PlantType.MELON_PULT;
            case "WINTER_MELON": return PlantType.WINTER_MELON;
            case "KERNEL_PULT": return PlantType.KERNEL_PULT;
            case "COB_CANNON": return PlantType.COB_CANNON;
            case "PEANUT": return PlantType.PEANUT;
            case "CITRON": return PlantType.CITRON;
            case "CHERRY_BOMB": return PlantType.CHERRY_BOMB;
            case "POTATO_MINE": return PlantType.POTATO_MINE;
            case "JALAPENO": return PlantType.JALAPENO;
            case "EXPLODE_O_NUT": return PlantType.EXPLODE_O_NUT;
            case "CHOMPER": return PlantType.CHOMPER;
            case "BONK_CHOY": return PlantType.BONK_CHOY;
            case "SQUASH": return PlantType.SQUASH;
            case "WALL_NUT": return PlantType.WALL_NUT;
            case "TALL_NUT": return PlantType.TALL_NUT;
            case "PUMPKIN": return PlantType.PUMPKIN;
            case "GARLIC": return PlantType.GARLIC;
            case "SWEET_POTATO": return PlantType.SWEET_POTATO;
            case "SPORE_SHROOM": return PlantType.SPORE_SHROOM;
            case "MAGNET_SHROOM": return PlantType.MAGNET_SHROOM;
            case "HYPNO_SHROOM": return PlantType.HYPNO_SHROOM;
            case "LASER_BEAN": return PlantType.LASER_BEAN;
            case "FUME_SHROOM": return PlantType.FUME_SHROOM;
            case "HOMING_THISTLE": return PlantType.HOMING_THISTLE;
            case "SNAPDRAGON": return PlantType.SNAPDRAGON;
            case "SPEARMINT": return PlantType.SPEARMINT;
            case "FROSTBITE_CAVES_MINT": return PlantType.FROSTBITE_CAVES_MINT;
            case "TORCHWOOD_MINT": return PlantType.TORCHWOOD_MINT;
            case "LILY_PAD": return PlantType.LILY_PAD;
            case "TANGLE_KELP": return PlantType.TANGLE_KELP;
            case "WALLNUT_BOWLING": return PlantType.WALLNUT_BOWLING;
            case "EXPLODE_O_NUT_BOWLING": return PlantType.EXPLODE_O_NUT_BOWLING;
            case "BIG_WALLNUT": return PlantType.BIG_WALLNUT;
            case "PUFF_SHROOM": return PlantType.PUFF_SHROOM;
            default: return null;
        }
    }


    /**
     * لیست تمام گیاهان تعریف‌شده در بازی را برمی‌گرداند.
     * @return لیست انواع گیاه
     */
    public List<PlantType> getAllPlants() {
        List<PlantType> allPlants = Arrays.asList(PlantType.values());
        return allPlants;
    }

    /**
     * لیست زامبی‌های دیده‌شده کاربر را برمی‌گرداند.
     * @param user کاربر
     * @return لیست انواع زامبی
     */
    public List<ZombieType> getSeenZombies(User user) { return null; }

    /**
     * لیست تمام زامبی‌های تعریف‌شده در بازی را برمی‌گرداند.
     * @return لیست انواع زامبی
     */
    public List<ZombieType> getAllZombies() {
        List<ZombieType> allZombies = Arrays.asList(ZombieType.values());
        return allZombies;
    }

    /**
     * مشخصات یک گیاه را برمی‌گرداند.
     * @param type نوع گیاه
     * @return نمونه گیاه با مشخصات کامل
     */
    public Plant getPlantDetails(PlantType type) { return null; }

    /**
     * مشخصات یک زامبی را برمی‌گرداند.
     * @param type نوع زامبی
     * @return نمونه زامبی با مشخصات کامل
     */
    public Zombie getZombieDetails(ZombieType type) { return null; }

    /**
     * گیاه را ارتقا می‌دهد (سکه + seed packet لازم است).
     * @param user کاربر
     * @param type نوع گیاه
     * @throws exception.GameException اگر منابع کافی نباشد
     */
    public void upgradePlant(User user, PlantType type) { }

    /**
     * یک گیاه جدید خریداری می‌کند (2000 سکه).
     * @param user کاربر
     * @param type نوع گیاه
     * @throws exception.GameException اگر سکه کافی نباشد یا قبلاً خریده
     */
    public void purchasePlant(User user, PlantType type) {
        user.setCoins(user.getCoins()-2000);
        user.getUnlockedPlants().add(type.name());
    }

    /**
     * هزینه ارتقای بعدی گیاه را محاسبه می‌کند.
     * هزینه با هر ارتقا افزایش می‌یابد.
     * @param user کاربر
     * @param type نوع گیاه
     * @return (سکه لازم، seed packet لازم)
     */
    public int[] getUpgradeCost(User user, PlantType type) { return null; }
}
