package com.pvz2.graphics.net;

/**
 * The fixed set of in-match reactions (doc §"سیستم ارسال واکنش"): 3 text messages
 * + 3 emojis (mandatory) + 3 animated stickers (bonus). All English/graphical.
 */
public final class ReactionCatalog {

    private ReactionCatalog() { }

    public static final String KIND_TEXT = "TEXT";
    public static final String KIND_EMOJI = "EMOJI";
    public static final String KIND_STICKER = "STICKER";

    /** 3 ready-made text messages. */
    public static final String[] TEXTS = {
            "Good game!", "Nice move!", "Too easy!"
    };

    /** 3 quick emojis. */
    public static final String[] EMOJIS = {
            "😎", // 😎
            "😂", // 😂
            "😡"  // 😡
    };

    /** 3 animated stickers (glyph doubles as id; rendered with a pop+pulse). */
    public static final String[] STICKERS = {
            "☀️",       // ☀️ sun
            "🧠",       // 🧠 brain
            "🌻"        // 🌻 sunflower
    };
}
