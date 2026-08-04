package com.pvz2.model;

import com.pvz2.model.enums.QuestType;
import com.pvz2.model.enums.RewardType;

/**
 * یک کوئست (مأموریت) در بازی.
 * شرط تکمیل و پاداش دارد.
 */
public class Quest {

    /** شناسه یکتا کوئست */
    private String id;

    /** دسته‌بندی کوئست */
    private QuestType questType;

    /** عنوان کوئست */
    private String title;

    /** توضیح شرط تکمیل */
    private String description;

    /** پیشرفت فعلی (مثلاً 3 از 10 زامبی کشته) */
    private int currentProgress;

    /** هدف (مثلاً 10) */
    private int targetProgress;

    /** آیا تکمیل شده */
    private boolean completed;

    /** آیا پاداش گرفته شده */
    private boolean rewardClaimed;

    /** نوع پاداش */
    private RewardType rewardType;

    /** مقدار پاداش */
    private int rewardAmount;

    /** آیا روزانه است (هر روز ریست می‌شود) */
    private boolean daily;

    /** تاریخ آخرین ریست (برای کوئست‌های روزانه) */
    private String lastResetDate;

    public Quest(String id, QuestType questType, String title,
                 String description, int targetProgress) {
        this.id = id;
        this.questType = questType;
        this.title = title;
        this.description = description;
        this.targetProgress = targetProgress;
        this.currentProgress = 0;
        this.completed = false;
        this.rewardClaimed = false;
    }

    /** پیشرفت را اضافه می‌کند و تکمیل را بررسی می‌کند */
    public void addProgress(int amount) { }

    /** بررسی می‌کند آیا کوئست تکمیل شده */
    public boolean isCompleted() { return completed; }

    public String getId() { return id; }
    public QuestType getQuestType() { return questType; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getCurrentProgress() { return currentProgress; }
    public int getTargetProgress() { return targetProgress; }
    public boolean isRewardClaimed() { return rewardClaimed; }
    public void setRewardClaimed(boolean rewardClaimed) { this.rewardClaimed = rewardClaimed; }
    public RewardType getRewardType() { return rewardType; }
    public void setRewardType(RewardType rewardType) { this.rewardType = rewardType; }
    public int getRewardAmount() { return rewardAmount; }
    public void setRewardAmount(int rewardAmount) { this.rewardAmount = rewardAmount; }
    public boolean isDaily() { return daily; }
    public void setDaily(boolean daily) { this.daily = daily; }
    public String getLastResetDate() { return lastResetDate; }
    public void setLastResetDate(String date) { this.lastResetDate = date; }
}
