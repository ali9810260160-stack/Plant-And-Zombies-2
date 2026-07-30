package model;

/**
 * تعریف یک کوئست — از quests.json بارگذاری می‌شود.
 */
public class QuestDefinition {

    public enum QuestType { DAILY, STORY, EPIC, REPEATABLE }
    public enum RewardType { COIN, GEM, SEED_PACKET, RANDOM_PLANT }
    public enum Priority { LOW, MEDIUM, HIGH, CRITICAL }
    public enum Condition {
        COLLECT_SUN, KILL_CHAPTER_ZOMBIES, KILL_WITH_PLANT,
        KILL_WITH_CACTUS, WIN_WITH_MAX_PLANT_LOSS, WIN_WITH_ZERO_SUN,
        KILL_10_IN_30SEC, USE_EXPLOSIVES, WIN_WITH_SYMMETRIC_GARDEN,
        KILL_WITH_FAMILY, WIN_WITHOUT_FAMILY, WIN_DAY_WITH_NIGHT_PLANTS,
        WIN_STREAK, KILL_NEAR_HOME, WIN_WITH_ASYMMETRIC_GARDEN,
        WIN_WITH_MAX_SUN_PRODUCERS, WIN_WITH_EMPTY_COLUMN,
        WIN_WITH_EMPTY_ROW, WIN_WITH_EMPTY_CROSS, LAWNMOWER_KILLS
    }

    private final String id;
    private final String nameFA;
    private final QuestType type;
    private final Condition condition;
    private final String description;
    private final RewardType rewardType;
    private final int rewardBase;
    private final boolean rewardMultiplier;
    private final Priority priority;
    private final int[] targets;

    public QuestDefinition(String id, String nameFA, QuestType type,
                           Condition condition, String description,
                           RewardType rewardType, int rewardBase,
                           boolean rewardMultiplier, Priority priority,
                           int[] targets) {
        this.id = id;
        this.nameFA = nameFA;
        this.type = type;
        this.condition = condition;
        this.description = description;
        this.rewardType = rewardType;
        this.rewardBase = rewardBase;
        this.rewardMultiplier = rewardMultiplier;
        this.priority = priority;
        this.targets = targets;
    }

    public String getId() { return id; }
    public String getNameFA() { return nameFA; }
    public QuestType getType() { return type; }
    public Condition getCondition() { return condition; }
    public String getDescription() { return description; }
    public RewardType getRewardType() { return rewardType; }
    public int getRewardBase() { return rewardBase; }
    public boolean isRewardMultiplier() { return rewardMultiplier; }
    public Priority getPriority() { return priority; }
    public int[] getTargets() { return targets; }
    public int getTargetForLevel(int level) {
        if (targets == null || targets.length == 0) {
            return 1;
        }
        int idx = Math.max(0, Math.min(level - 1, targets.length - 1));
        return targets[idx];
    }
}
