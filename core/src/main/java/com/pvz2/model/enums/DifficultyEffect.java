package com.pvz2.model.enums;

/**
 * پارامترهای بازی که با تغییر سختی ضریب می‌گیرند.
 * ضریب افزایش: dl/3 ؛ ضریب کاهش: 3/dl (dl = difficulty level)
 */
public enum DifficultyEffect {
    ZOMBIE_HEALTH,      // جان زامبی‌ها (افزایش)
    WAVE_COST,          // هزینه موج زامبی (کاهش)
    ZOMBIE_DAMAGE,      // دمیج زامبی (افزایش)
    SUN_FALL_RATE,      // نرخ ریزش خورشید از آسمان (کاهش)
    GAME_SPEED          // سرعت کلی بازی (افزایش)
}
