package model.enums;

/**
 * افکت‌های وضعیتی که می‌توانند روی گیاه اعمال شوند.
 */
public enum PlantEffect {
    FROZEN,         // یخ‌زده (توسط Hunter Zombie یا باد یخی)
    OCTOPUSED,      // اختاپوس روی سر (مشابه یخ‌زدگی)
    WIZARDED,       // به گربه تبدیل شده (توسط Wizard Zombie)
    ICE_WIND_LVL1,  // سطح اول باد یخی (بدون اثر هنوز)
    ICE_WIND_LVL2   // سطح دوم باد یخی (یک مرحله به یخ‌زدگی)
}
