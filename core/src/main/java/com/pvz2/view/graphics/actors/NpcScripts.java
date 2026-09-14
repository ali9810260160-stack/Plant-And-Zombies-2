package com.pvz2.graphics.actors;

import com.pvz2.model.enums.LevelType;

import java.util.ArrayList;
import java.util.List;

import com.pvz2.graphics.actors.NpcDialogOverlay.Line;

/**
 * متنِ گفت‌وگوهای ان‌پی‌سی (Crazy Dave ↔ Winnie) در آغازِ مراحل (BX3).
 * بر اساسِ فصل، نوعِ مرحله و شماره‌ی مرحله یک گفت‌وگوی کوتاه برمی‌گرداند.
 */
public final class NpcScripts {

    private NpcScripts() {}

    /** آیا برای این نوعِ مرحله باید گفت‌وگوی ان‌پی‌سی نمایش داده شود؟ */
    public static boolean shouldShow(LevelType type) {
        // فقط برای مراحلِ داستانیِ ماجراجویی؛ مینی‌گیم‌ها/ویژه‌ها مستقیم به مأموریت می‌روند
        return type == LevelType.NORMAL || type == LevelType.BOSS;
    }

    public static List<Line> forLevel(String chapter, LevelType type, int levelNumber) {
        List<Line> s = new ArrayList<>();
        String ch = chapter == null ? "" : chapter.toLowerCase();

        if (type == LevelType.BOSS) {
            s.add(new Line(true,  "Winnie! The Zomboss is here! And he brought his BIG robot!"));
            s.add(new Line(false, "Dave, that machine is enormous. Its armor has three layers."));
            s.add(new Line(true,  "Three layers of TACOS?! ...wait, no, layers of ROBOT. Right."));
            s.add(new Line(false, "Focus. Break every segment of its health and we win. Good luck."));
            return s;
        }

        if (levelNumber <= 1) {
            // اولین مرحله‌ی فصل — معرفی
            if (ch.contains("egypt")) {
                s.add(new Line(true,  "Ancient Egypt! Smell that? That's the smell of old zombies!"));
                s.add(new Line(false, "Sensors confirm undead pharaohs approaching from the east, Dave."));
                s.add(new Line(true,  "Plant your seeds, neighbor! Don't let 'em reach my taco... I mean, house!"));
                s.add(new Line(false, "Collect sun, plant wisely, and hold the lawn. Beginning mission."));
            } else if (ch.contains("pirate")) {
                s.add(new Line(true,  "Arrr! Pirate Seas! Zombies on boats — that's just cheating!"));
                s.add(new Line(false, "They will swing across the gaps, Dave. Mind the open planks."));
                s.add(new Line(true,  "Then we plant HARDER. Yarrr-ticultuere! Get it? Ha!"));
                s.add(new Line(false, "...Deploying anyway. Defend the house."));
            } else {
                s.add(new Line(true,  "New neighborhood, new zombies! I LOVE this job!"));
                s.add(new Line(false, "A fresh horde is forming, Dave. Recommend immediate defense."));
                s.add(new Line(true,  "You heard the car! Plant your plants and protect your brain!"));
                s.add(new Line(false, "Objective: do not let a single zombie reach the house."));
            }
            return s;
        }

        // مراحلِ میانیِ فصل — گفت‌وگوی کوتاه‌ترِ متغیر
        s.add(new Line(true,  "They're back for round " + levelNumber + "! Persistent little brain-eaters."));
        s.add(new Line(false, "This wave reads as larger than the last. Stay sharp, Dave."));
        s.add(new Line(true,  "Sharp? I sharpened my spoon this morning! We're READY!"));
        return s;
    }
}
