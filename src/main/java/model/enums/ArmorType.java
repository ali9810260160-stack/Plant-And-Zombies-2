package model.enums;

/**
 * انواع زره‌هایی که زامبی‌ها می‌توانند داشته باشند.
 * زره باید قبل از آسیب به خود زامبی از بین برود.
 */
public enum ArmorType {
    CONE,           // مخروط (370 HP)
    BUCKET,         // سطل (1100 HP) - magnetshroom جذب می‌کند
    HELMET,         // کلاهخود شوالیه (1600 HP) - magnetshroom جذب می‌کند
    SHOULDER_ARMOR, // شانه‌بند شوالیه (1600 HP)
    BLOCK,          // بلوک روی سر (2200 HP)
    NEWSPAPER,      // روزنامه (HP معادل زامبی عادی)
    BARREL          // دبه (جلوی زامبی را می‌گیرد، تیر رد نمی‌شود)
}
