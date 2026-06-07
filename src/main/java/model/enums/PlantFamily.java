package model.enums;

/**
 * دسته‌بندی اصلی گیاهان بر اساس نقش در بازی.
 * برای UML و منطق تعامل گیاهان با یکدیگر و زامبی‌ها کاربرد دارد.
 */
public enum PlantFamily {
    SUN_PRODUCER,     // تولیدکننده خورشید
    SHOOTER,          // شلیک‌کننده مستقیم
    LOBBER,           // پرتاب‌کننده هوایی
    EXPLOSIVE,        // گیاه انفجاری
    MELEE_ATTACKER,   // مبارز تن‌به‌تن
    WALL_NUT,         // گیاه دفاعی/سپر
    MODIFIER,         // گیاه پشتیبان
    STRIKE_THROUGH,   // نفوذکننده
    HOMING,           // ردیاب
    MINT              // نعناع (اثر فوری روی هم‌خانواده)
}
