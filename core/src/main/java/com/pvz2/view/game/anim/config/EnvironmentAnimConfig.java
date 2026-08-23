package com.pvz2.view.game.anim.config;

/**
 * تنظیمات یک عنصر محیطی متحرک (سنگ قبر، مورر، گردباد، بلوک یخ، آب).
 *
 * ═══ ویژگی جدید نسخه ۲ ═══
 * این عناصر بر خلاف گیاه/زامبی، به یک Tile یا کل صفحه مرتبط‌اند نه به
 * یک entity متحرک با state machine پیچیده — به همین دلیل ساختار
 * ساده‌تری دارند: فقط یک pamPath و لیستی از clip های موجود.
 * توسط GridRenderer برای رندر واقعی (به‌جای فقط مستطیل رنگی) استفاده
 * می‌شود.
 */
public class EnvironmentAnimConfig {

    /** مسیر PAM از ریشه assets. */
    public String pamPath = "";

    /** نام‌های clip موجود در این PAM (مرجع سریع، از environment_animations.json). */
    public java.util.List<String> clips = new java.util.ArrayList<>();
}
