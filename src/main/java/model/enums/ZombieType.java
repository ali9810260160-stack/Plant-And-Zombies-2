package model.enums;

/**
 * انواع زامبی‌های موجود در بازی.
 * زامبی‌های مشترک بین چپترها + زامبی‌های اختصاصی هر فصل.
 */
public enum ZombieType {
    // --- مشترک بین همه چپترها ---
    NORMAL,             // زامبی معمولی
    CONEHEAD,           // سرمخروطی
    BUCKETHEAD,         // سرسطلی
    KNIGHT,             // شوالیه (کلاهخود + شانه‌بند)
    BLOCKHEAD,          // سربلوکی
    GARGANTUAR,         // زامبی غول‌پیکر
    IMP,                // ایمپ (کوچک و سریع)
    ALL_STAR,           // فوتبالیست
    ARCADE_ZOMBIE,      // زامبی آرکید
    PARASOL_ZOMBIE,     // چتردار (دفع لابر)
    TURQUOISE_ZOMBIE,   // تورکوایز (خورشیددزد + لیزر)
    PROSPECTOR_ZOMBIE,  // اکتشافگر (دینامیت)
    PIANIST_ZOMBIE,     // پیانیست (جابجایی تصادفی)
    NEWSPAPER_ZOMBIE,   // پیرمرد روزنامه‌دار
    BARREL_ROLLER,      // زامبی دبه‌ای

    // --- مصر باستان ---
    RA_ZOMBIE,          // خورشیددزد مصری
    EXPLORER_ZOMBIE,    // مشعل‌دار
    TOMB_RAISER,        // قبرساز

    // --- غارهای یخی ---
    DODO_RIDER,         // سوار بر دودو (پرنده)
    HUNTER_ZOMBIE,      // شکارچی (یخ پرت می‌کند)
    TROGLOBITE,         // تروگلوبایت (یخ هل می‌دهد)

    // --- ساحل ---
    FISHERMAN_ZOMBIE,   // ماهیگیر
    SNORKEL_ZOMBIE,     // غواص
    OCTOPUS_ZOMBIE,     // اختاپوس‌پرت‌کن

    // --- قرون وسطی ---
    JESTER_ZOMBIE,      // ژانگولر (تیر برمی‌گرداند)
    WIZARD_ZOMBIE,      // جادوگر (تبدیل به گربه)
    KING_ZOMBIE,        // پادشاه (زامبی‌ها را ارتقا می‌دهد)
    DRAGON_IMP,         // ایمپ اژدها (مقاوم به آتش)

    // --- مینی‌گیم Zombotany ---
    ZOMBOTANY_PEASHOOTER,   // زامبی تیرانداز
    ZOMBOTANY_WALLNUT,      // زامبی گردو
    ZOMBOTANY_JALAPENO,     // زامبی فلفل
    ZOMBOTANY_SQUASH,       // زامبی کدو

    // --- مینی‌گیم I, Zombie ---
    SUN_PRODUCER_ZOMBIE     // زامبی تولید خورشید (مخصوص I, Zombie)
}
