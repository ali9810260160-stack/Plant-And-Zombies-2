package com.pvz2.model;

import java.util.ArrayList;
import java.util.List;

public class Greenhouse {

    /** ابعاد شبکه گلخانه — مطابق پس‌زمینه‌ی بازطراحی‌شده: ۳ ردیف × ۴ ستون = ۱۲ جایگاه. */
    public static final int ROWS = 3;
    public static final int COLS = 4;

    private Pot[][] pots;

    public Greenhouse() {
        this.pots = new Pot[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                Pot p = new Pot(c + 1, r + 1);
                p.setLocked(r > 0); // فقط ردیف اول به‌صورت پیش‌فرض باز است
                pots[r][c] = p;
            }
        }
    }

    public Pot getPot(int x, int y) {
        if (x < 1 || x > COLS || y < 1 || y > ROWS) return null;
        return pots[y - 1][x - 1];
    }

    public int getUnlockedCount() {
        int count = 0;
        for (Pot[] row : pots)
            for (Pot p : row)
                if (!p.isLocked()) count++;
        return count;
    }

    public List<Pot> getReadyPots() {
        List<Pot> result = new ArrayList<>();
        for (Pot[] row : pots)
            for (Pot p : row)
                if (!p.isLocked() && p.isReady()) result.add(p);
        return result;
    }

    public Pot[][] getPots() { return pots; }
}
