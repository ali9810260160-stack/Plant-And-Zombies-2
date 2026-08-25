package com.pvz2.map;

/**
 * انواع اشیاء (Objects) قابل تعریف در لایه‌های Object در Tiled.
 *
 * هر شیء باید Custom Property زیر را داشته باشد:
 *   pvz.type = [یکی از نام‌های enum زیر]
 *
 * برای توضیح کامل Property های هر نوع، به TILED_MAP_GUIDE.md مراجعه کنید.
 */
public enum MapObjectType {

    /** خانه قابل کاشت گیاه — Rectangle به اندازه یک cell */
    PLANT_SLOT,

    /** نقطه ورود زامبی از سمت راست */
    ZOMBIE_SPAWN_POINT,

    /** مرز خانه — game over اگر زامبی بدون آسیب رسد */
    HOUSE_BOUNDARY,

    /** جای لانمووِر در ابتدای هر لاین */
    LAWNMOWER_SLOT,

    /** بخشی از ریل (Rail) در مراحل ویژه */
    RAIL_SEGMENT,

    /** یک slot روی نوار نقاله (Conveyor Belt) */
    CONVEYOR_SLOT,

    /** محل ظهور قبر در قرون وسطی (توسط necromancer) */
    TOMBSTONE_SPAWN,

    /** المان محیطی ویژه (طوفان شن، باد یخ، موج، تاریکی) */
    SPECIAL_ELEMENT,

    /** خط سطح آب در ساحل */
    WATER_LINE_MARKER,

    /** ناحیه دوربین (FIXED یا EXTENDED) */
    CAMERA_REGION,

    /** بلوک یخ اولیه در غارهای یخی */
    ICE_BLOCK_SPAWN,

    /** نقطه سقوط خورشید */
    SUN_DROPPER,

    /** نشانگر عمومی مینی‌گیم */
    MINIGAME_MARKER,

    /** ناشناخته — هر property که پارس نشد */
    UNKNOWN;

    /**
     * تبدیل مقدار string property به enum.
     * @param value مقدار pvz.type از Tiled
     */
    public static MapObjectType fromString(String value) {
        if (value == null) return UNKNOWN;
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
