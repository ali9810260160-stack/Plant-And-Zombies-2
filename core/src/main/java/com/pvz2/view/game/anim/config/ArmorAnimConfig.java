package com.pvz2.view.game.anim.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * تنظیمات بصری یک نوع زره روی زامبی — نسخه ۲.
 *
 * ═══════════════════════════════════════════════════════════════════
 *  دو الگوی متفاوت armor (کشف‌شده از بررسی مستقیم assets واقعی):
 * ═══════════════════════════════════════════════════════════════════
 *
 * ── الگوی ۱: Overlay ساده (visMap) ──────────────────────────────────
 *   زره یک sprite اضافی روی همان اسکلت پایه است. طبق نمونه رسمی
 *   Demo.java از خود کتابخانه:
 *     visibilityMap.put("_zombie_egypt_armor2_states", true);
 *     visibilityMap.put("zombie_armor_bucket_norm", true);
 *   کاربرد: CONE, BUCKET, HELMET, SHOULDER_ARMOR, BLOCK
 *   نام‌های دقیق part باید با PamPartInspector یا Asset Browser کشف شوند.
 *
 * ── الگوی ۲: تعویض کامل clip (clip-suffix) ──────────────────────────
 *   بعضی زره‌ها یک ست کامل جدید از clip تعریف می‌کنند، نه فقط overlay:
 *
 *   مثال واقعی — Newspaper Zombie:
 *     دارای روزنامه: idle_newspaper, walk_newspaper, eat_newspaper
 *     بدون روزنامه:  idle,           walk,           eat
 *     لحظه از دست دادن: newspaper_defeat (یک‌بار پخش می‌شود)
 *
 *   مثال واقعی — Pharaoh/Sarcophagus Zombie:
 *     دارای تابوت: idle,      walk,      eat
 *     بدون تابوت:  idle_norm, walk_norm, eat_norm
 *     لحظه شکستن: break_power
 *
 * مثال JSON الگوی Overlay:
 * <pre>
 *   "CONE": { "visMap": { "zombie_armor_cone_norm": true } }
 * </pre>
 *
 * مثال JSON الگوی Clip-Suffix:
 * <pre>
 *   "NEWSPAPER": {
 *     "clipSuffixPresent": "_newspaper",
 *     "clipSuffixBroken": "",
 *     "breakEventClip": "newspaper_defeat"
 *   }
 * </pre>
 */
public class ArmorAnimConfig {

    /**
     * visibility map: نام sprite داخل PAM → نمایش داده شود یا نه.
     * مستقیماً به PamDrawUtil.draw(..., visMap) پاس داده می‌شود.
     * خالی یعنی این armor از الگوی clip-suffix استفاده می‌کند.
     */
    public Map<String, Boolean> visMap = new LinkedHashMap<>();

    /**
     * پسوندی که وقتی این armor هنوز سالم است به نام clip اضافه می‌شود.
     * مثال: "_newspaper". "" یعنی حالت سالم از نام پایه (بدون پسوند)
     * استفاده می‌کند (مثل Sarcophagus: سالم=walk، شکسته=walk_norm).
     */
    public String clipSuffixPresent = "";

    /**
     * پسوندی که وقتی این armor شکسته/از بین رفته به نام clip اضافه می‌شود.
     * مثال Sarcophagus: "_norm". مثال Newspaper: "" (حالت شکسته بدون پسوند).
     */
    public String clipSuffixBroken = "";

    /**
     * یک‌بار-پخش‌شونده: clip ویژه‌ای که فقط در لحظه شکستن این armor پخش
     * می‌شود، سپس به state عادی (با suffix جدید) برمی‌گردد.
     * مثال: "newspaper_defeat" یا "break_power". null = بدون گذار خاص.
     */
    public String breakEventClip = null;

    /**
     * اگر این armor clip اختصاصی برای WALK/EAT دارد (نه فقط suffix)
     * اینجا override کامل می‌شود. عموماً لازم نیست؛ فقط برای موارد نادر.
     */
    public String clipOverride = null;

    /** آیا این armor از الگوی overlay استفاده می‌کند؟ */
    public boolean isOverlayStyle() {
        return !visMap.isEmpty();
    }

    /** آیا این armor از الگوی clip-suffix استفاده می‌کند؟ */
    public boolean isClipSuffixStyle() {
        return !clipSuffixPresent.isEmpty() || !clipSuffixBroken.isEmpty();
    }
}
