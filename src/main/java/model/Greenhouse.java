package model;

import java.util.ArrayList;
import java.util.List;

public class Greenhouse {

    private Pot[][] pots;

    public Greenhouse() {
        this.pots = new Pot[4][5];
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 5; c++) {
                Pot p = new Pot(c + 1, r + 1);
                p.setLocked(r > 0);
                pots[r][c] = p;
            }
        }
    }

    public Pot getPot(int x, int y) {
        if (x < 1 || x > 5 || y < 1 || y > 4) return null;
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
