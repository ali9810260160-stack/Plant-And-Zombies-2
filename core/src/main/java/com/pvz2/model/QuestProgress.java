package com.pvz2.model;

/**
 * پیشرفت یک کاربر در یک کوئست.
 */
public class QuestProgress {

    private final String questId;
    private int currentLevel;
    private int currentValue;
    private boolean completed;
    private boolean claimed;
    private String lastResetDate;

    public QuestProgress(String questId) {
        this.questId = questId;
        this.currentLevel = 1;
        this.currentValue = 0;
        this.completed = false;
        this.claimed = false;
        this.lastResetDate = "";
    }

    public void increment(int amount) {
        if (!completed) {
            currentValue += amount;
        }
    }

    public void reset() {
        currentValue = 0;
        completed = false;
        claimed = false;
    }

    public String getQuestId() { return questId; }
    public int getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(int lvl) { this.currentLevel = lvl; }
    public int getCurrentValue() { return currentValue; }
    public void setCurrentValue(int v) { this.currentValue = v; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public boolean isClaimed() { return claimed; }
    public void setClaimed(boolean claimed) { this.claimed = claimed; }
    public String getLastResetDate() { return lastResetDate; }
    public void setLastResetDate(String d) { this.lastResetDate = d; }
}
