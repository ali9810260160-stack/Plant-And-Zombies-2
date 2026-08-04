package com.pvz2.model.enums;

/**
 * دسته‌بندی کوئست‌ها در منوی Travel Log.
 * هر صفحه Travel Log یک دسته کوئست را نمایش می‌دهد.
 */
public enum QuestType {
    ADVENTURE,      // کوئست‌های داستانی (اولویت بحرانی)
    SPECIAL,        // چالش‌های Epic (اولویت بالا، پاداش الماس)
    MINIGAME,       // مرتبط با مینی‌گیم‌ها
    COMMUNITY,      // کوئست‌های جامعه/شبکه
    CHALLENGE,      // چالش‌های تکرارپذیر
    MYSTERY         // کوئست‌های مرموز/روزانه
}
