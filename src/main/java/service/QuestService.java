package service;

import model.*;
import model.QuestDefinition.*;
import model.enums.ChapterType;
import model.enums.PlantFamily;
import model.enums.PlantType;
import repository.UserRepository;
import util.FileUtil;
import util.SimpleJsonParser;
import view.ConsoleView;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * سرویس کوئست — بارگذاری از JSON، ردیابی پیشرفت، جایزه.
 */
public class QuestService {

    private static final Map<String, QuestDefinition> QUESTS = new LinkedHashMap<>();

    static {
        loadQuestsFromJson();
    }

    private static void loadQuestsFromJson() {
        try {
            String json = FileUtil.readFile("data/quests.json");
            List<Map<String, String>> entries = SimpleJsonParser.parseArray(json);
            for (Map<String, String> m : entries) {
                QuestDefinition qd = parseQuest(m);
                if (qd != null) {
                    QUESTS.put(qd.getId(), qd);
                }
            }
            // quests loaded successfully
        } catch (Exception e) {
            System.err.println("[QuestService] Cannot load quests.json: "
                + e.getMessage());
            loadBuiltinQuests();
        }
    }

    private static QuestDefinition parseQuest(Map<String, String> m) {
        String id = m.getOrDefault("id", "");
        if (id.isEmpty()) {
            return null;
        }
        String nameFA  = m.getOrDefault("nameFA", id);
        String typeStr = m.getOrDefault("type", "DAILY");
        String condStr = m.getOrDefault("condition", "COLLECT_SUN");
        String desc    = m.getOrDefault("description", "Quest: " + id);
        String rewType = m.getOrDefault("rewardType", "COIN");
        int rewBase    = parseInt(m.get("rewardBase"), parseInt(m.get("rewardAmount"), 100));
        boolean mult   = "true".equalsIgnoreCase(m.getOrDefault("rewardMultiplier", "false"));
        String priStr  = m.getOrDefault("priority", "MEDIUM");

        QuestType type;
        Condition cond;
        RewardType rewEnum;
        Priority pri;
        try {
            type    = QuestType.valueOf(typeStr.trim().toUpperCase());
            cond    = Condition.valueOf(condStr.trim().toUpperCase());
            rewEnum = RewardType.valueOf(rewType.trim().toUpperCase());
            pri     = Priority.valueOf(priStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }

        String targetsRaw = m.getOrDefault("targets", "[]")
                             .replace("[","").replace("]","");
        int[] targets = Arrays.stream(targetsRaw.split(","))
                .filter(s -> !s.trim().isEmpty())
                .mapToInt(s -> {
                    try { return (int) Double.parseDouble(s.trim()); }
                    catch (NumberFormatException ex) { return 0; }
                })
                .filter(v -> v > 0)
                .toArray();

        return new QuestDefinition(id, nameFA, type, cond, desc,
                rewEnum, rewBase, mult, pri, targets);
    }

    private static int parseInt(String s, int def) {
        if (s == null || s.isEmpty() || s.equals("null")) {
            return def;
        }
        try {
            return (int) Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    // ─── User quest state management ────────────────────────────

    private final UserRepository userRepository;
    private final UserService userService;
    private final ConsoleView view;
    private Map<String, QuestProgress> activeProgress;
    private User currentUser;

    public QuestService(UserRepository userRepository,
                        UserService userService, ConsoleView view) {
        this.userRepository = userRepository;
        this.userService    = userService;
        this.view           = view;
        this.activeProgress = new LinkedHashMap<>();
    }

    public void loadForUser(User user) {
        this.currentUser = user;
        this.activeProgress = new LinkedHashMap<>();
        // init all quests
        for (String id : QUESTS.keySet()) {
            activeProgress.put(id, new QuestProgress(id));
        }
        // Reset daily quests if new day
        String today = LocalDate.now().toString();
        for (Map.Entry<String, QuestProgress> e : activeProgress.entrySet()) {
            QuestDefinition qd = QUESTS.get(e.getKey());
            if (qd != null && qd.getType() == QuestType.DAILY) {
                if (!today.equals(e.getValue().getLastResetDate())) {
                    e.getValue().reset();
                    e.getValue().setLastResetDate(today);
                }
            }
        }
    }

    // ─── Event hooks (called from CombatService / GameService) ───

    public void onSunCollected(int amount) {
        updateQuest("DAILY_SUN_COLLECTOR", amount);
    }

    public void onZombieKilled(String zombieType, ChapterType chapter,
                                String killerPlantType) {
        String chapterName = chapter != null ? chapter.name() : "";
        updateQuest("CHAPTER_HUNTER_" + chapterName, 1);
        updateQuest("CHAPTER_HUNTER", 1);
        if (killerPlantType != null && !killerPlantType.isEmpty()) {
            updateQuest("DAILY_PLANT_KILLER", 1);
            if (killerPlantType.equals("CACTUS")) {
                updateQuest("DAILY_CACTUS_ONLY", 1);
            }
        }
        checkAndComplete();
    }

    public void onLawnmowerKill(int count) {
        updateQuest("EPIC_LAWNMOWER", count);
        checkAndComplete();
    }

    public void onExplosiveUsed() {
        updateQuest("DAILY_EXPLOSIVE_USER", 1);
        checkAndComplete();
    }

    public void onWaveKillsIn30Sec(int killCount) {
        if (killCount >= 10) {
            updateQuest("FAST_KILLER", 1);
            checkAndComplete();
        }
    }

    public void onGameWon(GameSession session) {
        checkWinConditionQuests(session);
        updateQuest("DAILY_WIN_STREAK", 1);
        checkAndComplete();
    }

    private void checkWinConditionQuests(GameSession session) {
        if (session == null || session.getGameMap() == null) {
            return;
        }
        if (session.getSunAmount() == 0) {
            updateQuest("EPIC_ZERO_SUN", 1);
        }
        if (session.getPlantsLost() == 0) {
            updateQuest("PLANT_SAVER", 1);
        }
        boolean hasNightPlantsOnly = checkNightPlantsOnly(session);
        if (hasNightPlantsOnly) {
            updateQuest("EPIC_NIGHT_PLANTS", 1);
        }
        checkEmptyColumn(session);
        checkEmptyRow(session);
    }

    private boolean checkNightPlantsOnly(GameSession session) {
        for (int r = 1; r <= session.getGameMap().getRows(); r++) {
            for (int c = 1; c <= session.getGameMap().getCols(); c++) {
                model.tiles.Tile tile = session.getGameMap().getTile(c, r);
                if (tile == null || tile.getPlant() == null) {
                    continue;
                }
                model.plants.Plant plant = tile.getPlant();
                if (!plant.hasTag(model.enums.PlantTag.NIGHT)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void checkEmptyColumn(GameSession session) {
        for (int c = 1; c <= session.getGameMap().getCols(); c++) {
            boolean empty = true;
            for (int r = 1; r <= session.getGameMap().getRows(); r++) {
                model.tiles.Tile tile = session.getGameMap().getTile(c, r);
                if (tile != null && tile.getPlant() != null) {
                    empty = false;
                    break;
                }
            }
            if (empty) {
                updateQuest("DAILY_EMPTY_COLUMN", 1);
                return;
            }
        }
    }

    private void checkEmptyRow(GameSession session) {
        for (int r = 1; r <= session.getGameMap().getRows(); r++) {
            boolean empty = true;
            for (int c = 1; c <= session.getGameMap().getCols(); c++) {
                model.tiles.Tile tile = session.getGameMap().getTile(c, r);
                if (tile != null && tile.getPlant() != null) {
                    empty = false;
                    break;
                }
            }
            if (empty) {
                updateQuest("DAILY_EMPTY_ROW", 1);
                return;
            }
        }
    }

    private void updateQuest(String questId, int amount) {
        QuestProgress p = activeProgress.get(questId);
        if (p == null || p.isCompleted()) {
            return;
        }
        p.increment(amount);
    }

    private void checkAndComplete() {
        for (Map.Entry<String, QuestProgress> e : activeProgress.entrySet()) {
            QuestDefinition qd = QUESTS.get(e.getKey());
            QuestProgress   qp = e.getValue();
            if (qd == null || qp.isCompleted()) {
                continue;
            }
            int target = qd.getTargetForLevel(qp.getCurrentLevel());
            if (qp.getCurrentValue() >= target) {
                qp.setCompleted(true);
                notifyQuestComplete(qd, qp);
            }
        }
    }

    private void notifyQuestComplete(QuestDefinition qd, QuestProgress qp) {
        view.printSuccess("🎉 Quest completed: ["
            + qd.getType() + "] " + qd.getNameFA());
        view.printInfo("  " + qd.getDescription()
            .replace("{target}", String.valueOf(
                qd.getTargetForLevel(qp.getCurrentLevel()))));
        giveReward(qd, qp);
        if (currentUser != null) {
            if (qd.getType() == QuestType.DAILY) {
                currentUser.setDailyQuestsCompleted(
                    currentUser.getDailyQuestsCompleted() + 1);
            } else {
                currentUser.setRegularQuestsCompleted(
                    currentUser.getRegularQuestsCompleted() + 1);
            }
            userRepository.save(currentUser);
        }
    }

    private void giveReward(QuestDefinition qd, QuestProgress qp) {
        int level = qp.getCurrentLevel();
        int amount = qd.isRewardMultiplier()
                     ? qd.getRewardBase() * level
                     : qd.getRewardBase();
        if (currentUser == null) {
            return;
        }
        switch (qd.getRewardType()) {
            case COIN:
                currentUser.setCoins(currentUser.getCoins() + amount);
                view.printInfo("  Reward: +" + amount + " coins 💰");
                break;
            case GEM:
                currentUser.setGems(currentUser.getGems() + amount);
                view.printInfo("  Reward: +" + amount + " gems 💎");
                break;
            case SEED_PACKET:
                view.printInfo("  Reward: +" + amount + " seed packets 🌱");
                break;
            case RANDOM_PLANT:
                view.printInfo("  Reward: 1 random plant unlock! 🌿");
                break;
            default:
                break;
        }
    }

    // ─── Display methods ─────────────────────────────────────────

    public void showPage(String page) {
        List<QuestDefinition> filtered = filterByPage(page);
        filtered.sort(Comparator.comparing(QuestDefinition::getPriority).reversed());

        view.printHeader("📋 Quests — " + page.toUpperCase());
        if (filtered.isEmpty()) {
            view.printInfo("No quests in this category.");
            return;
        }
        for (QuestDefinition qd : filtered) {
            QuestProgress qp = activeProgress.getOrDefault(
                qd.getId(), new QuestProgress(qd.getId()));
            printQuest(qd, qp);
        }
    }

    private void printQuest(QuestDefinition qd, QuestProgress qp) {
        String status = qp.isCompleted()
            ? ConsoleView.GREEN + "[DONE]" + ConsoleView.RESET
            : ConsoleView.RED   + "[TODO]" + ConsoleView.RESET;
        int target = qd.getTargetForLevel(qp.getCurrentLevel());
        String prog = qp.getCurrentValue() + "/" + target;
        int rew = qd.isRewardMultiplier()
                  ? qd.getRewardBase() * qp.getCurrentLevel()
                  : qd.getRewardBase();
        String desc = qd.getDescription()
            .replace("{target}", String.valueOf(target))
            + "  (" + prog + ")";
        view.getTravelLogView().printQuestRow(status, qd.getPriority().toString(),
            qd.getNameFA(), desc, rew, qd.getRewardType().toString());
    }

    private List<QuestDefinition> filterByPage(String page) {
        return QUESTS.values().stream()
            .filter(qd -> {
                switch (page.toLowerCase()) {
                    case "story":      return qd.getType() == QuestType.STORY;
                    case "daily":      return qd.getType() == QuestType.DAILY;
                    case "epic":       return qd.getType() == QuestType.EPIC;
                    case "repeatable": return qd.getType() == QuestType.REPEATABLE;
                    default: return true;
                }
            })
            .collect(Collectors.toList());
    }

    public List<QuestDefinition> getAllQuests() {
        return new ArrayList<>(QUESTS.values());
    }

    public Map<String, QuestProgress> getActiveProgress() {
        return activeProgress;
    }

    private static void loadBuiltinQuests() {
        QUESTS.put("DAILY_SUN_COLLECTOR",
            new QuestDefinition("DAILY_SUN_COLLECTOR","آفتاب‌گیر روزانه",
                QuestType.DAILY, Condition.COLLECT_SUN,
                "جمع‌آوری {target} واحد خورشید",
                RewardType.COIN, 100, true, Priority.MEDIUM,
                new int[]{3000,4000,5000}));
        QUESTS.put("CHAPTER_HUNTER",
            new QuestDefinition("CHAPTER_HUNTER","شکارچی فصل",
                QuestType.STORY, Condition.KILL_CHAPTER_ZOMBIES,
                "شکست دادن 50 زامبی",
                RewardType.SEED_PACKET, 10, false, Priority.HIGH,
                new int[]{50}));
        QUESTS.put("EPIC_ZERO_SUN",
            new QuestDefinition("EPIC_ZERO_SUN","استاد دفاع",
                QuestType.EPIC, Condition.WIN_WITH_ZERO_SUN,
                "اتمام یک مرحله دقیقاً با صفر خورشید",
                RewardType.GEM, 200, false, Priority.CRITICAL,
                new int[]{1}));
        QUESTS.put("EPIC_LAWNMOWER",
            new QuestDefinition("EPIC_LAWNMOWER","وقت چمن‌زنی",
                QuestType.EPIC, Condition.LAWNMOWER_KILLS,
                "حداقل {target} زامبی را با چمن‌زن بکش",
                RewardType.GEM, 1, true, Priority.MEDIUM,
                new int[]{10,20,30,40,50}));
        QUESTS.put("PLANT_SAVER",
            new QuestDefinition("PLANT_SAVER","گیاه‌خوار اقتصادی",
                QuestType.STORY, Condition.WIN_WITH_MAX_PLANT_LOSS,
                "برد بدون از دست دادن بیش از {target} گیاه",
                RewardType.SEED_PACKET, 20, true, Priority.HIGH,
                new int[]{0,1,2,3,4,5}));
    }
}
