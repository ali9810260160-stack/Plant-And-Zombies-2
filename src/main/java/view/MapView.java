package view;

import model.GameSession;
import model.Sun;
import model.enums.TileType;
import model.plants.Plant;
import model.tiles.Tile;
import model.zombies.Zombie;

import java.util.List;

/**
 * نمایش ترمینالی نقشه بازی با رنگ ANSI.
 * هر خانه با کاراکترهای Unicode نشان داده می‌شود.
 */
public class MapView {

    private static final String RESET   = ConsoleView.RESET;
    private static final String BOLD    = ConsoleView.BOLD;
    private static final String RED     = ConsoleView.RED;
    private static final String GREEN   = ConsoleView.GREEN;
    private static final String YELLOW  = ConsoleView.YELLOW;
    private static final String BLUE    = ConsoleView.BLUE;
    private static final String MAGENTA = ConsoleView.MAGENTA;
    private static final String CYAN    = ConsoleView.CYAN;
    private static final String WHITE   = ConsoleView.WHITE;

    public void printMap(GameSession session) {
        printHeader(session);
        printTopBorder(session.getGameMap().getCols());
        for (int r = 1; r <= session.getGameMap().getRows(); r++) {
            printRow(r, session);
        }
        printBottomBorder(session.getGameMap().getCols());
        printLawnMowerStatus(session);
        printLegend();
    }

    private void printHeader(GameSession session) {
        System.out.println();
        System.out.println(BOLD + CYAN
            + "╔══════════════════════ GAME STATUS ═══════════════════════╗" + RESET);
        System.out.printf(CYAN + "║  " + RESET
            + YELLOW + "☀ Sun: %-5d" + RESET
            + "  " + MAGENTA + "🌿 Plant Food: %d/3" + RESET
            + "  " + RED + "🌊 Wave: %d/%d" + RESET
            + "  " + GREEN + "💀 Killed: %d" + RESET + CYAN + "   ║%n" + RESET,
            session.getSunAmount(),
            session.getPlantFoodCount(),
            session.getCurrentWaveIndex() + 1,
            session.getWaves() != null ? session.getWaves().size() : 0,
            session.getZombiesKilled());
        System.out.println(BOLD + CYAN
            + "╚═══════════════════════════════════════════════════════════╝" + RESET);
    }

    private void printTopBorder(int cols) {
        System.out.print(CYAN + "    ┌");
        for (int c = 0; c < cols; c++) {
            System.out.print("────");
            if (c < cols - 1) {
                System.out.print("┬");
            }
        }
        System.out.println("┐" + RESET);
    }

    private void printBottomBorder(int cols) {
        System.out.print(CYAN + "    └");
        for (int c = 0; c < cols; c++) {
            System.out.print("────");
            if (c < cols - 1) {
                System.out.print("┴");
            }
        }
        System.out.println("┘" + RESET);
    }

    private void printRow(int row, GameSession session) {
        int cols = session.getGameMap().getCols();
        System.out.printf(CYAN + " %2d │" + RESET, row);
        for (int col = 1; col <= cols; col++) {
            Tile tile = session.getGameMap().getTile(col, row);
            String cell = buildCell(tile, col, row, session);
            System.out.print(cell);
            if (col < cols) {
                System.out.print(CYAN + "│" + RESET);
            }
        }
        System.out.print(CYAN + "│" + RESET);
        printRowSideInfo(row, session);
        System.out.println();
    }

    private String buildCell(Tile tile, int col, int row, GameSession session) {
        if (tile == null) {
            return "    ";
        }
        String bg = getTileBackground(tile.getType());
        String content = getCellContent(tile, col, row, session);
        return bg + String.format("%-4s", content) + RESET;
    }

    private String getTileBackground(TileType type) {
        switch (type) {
            case WATER:         return "\u001B[44m";
            case TOMBSTONE:
            case DARK_TOMBSTONE: return "\u001B[100m";
            case ICY_GROUND:    return "\u001B[46m";
            case DARK_NORMAL:   return "\u001B[40m";
            case CAVE_NORMAL:   return "\u001B[100m";
            default:            return "";
        }
    }

    private String getCellContent(Tile tile, int col, int row,
                                   GameSession session) {
        StringBuilder sb = new StringBuilder();
        Zombie zombie = getZombieOnTile(col, row, session);
        Plant plant = tile.getPlant();
        boolean hasSun = hasSunAt(col, row, session);

        if (tile.isTombstone()) {
            sb.append(BOLD + WHITE + "⛰" + RESET);
        }
        if (plant != null) {
            sb.append(getPlantSymbol(plant));
        }
        if (zombie != null) {
            sb.append(getZombieSymbol(zombie));
        }
        if (hasSun) {
            sb.append(YELLOW + "☀" + RESET);
        }
        if (tile.getType() == TileType.WATER && plant == null && zombie == null) {
            sb.append(BLUE + "≈≈" + RESET);
        }
        return sb.length() == 0 ? "  " : sb.toString();
    }

    private String getPlantSymbol(Plant plant) {
        String cat = "";
        if (plant instanceof model.plants.GenericPlant) {
            model.plants.GenericPlant gp = (model.plants.GenericPlant) plant;
            cat = gp.getStats().getCategory();
        }
        String name = plant.getType().name();
        if (plant.isFrozen()) {
            return CYAN + BOLD + "❄" + RESET;
        }
        switch (name) {
            case "SUNFLOWER":       return GREEN + "🌻" + RESET;
            case "TWIN_SUNFLOWER":  return GREEN + "🌼" + RESET;
            case "PEASHOOTER":      return GREEN + "🌿" + RESET;
            case "REPEATER":        return GREEN + "🌱" + RESET;
            case "SNOW_PEA":        return CYAN  + "❄P" + RESET;
            case "WALL_NUT":        return YELLOW + "⊙" + RESET;
            case "TALL_NUT":        return YELLOW + "⊕" + RESET;
            case "CHERRY_BOMB":     return RED    + "💣" + RESET;
            case "POTATO_MINE":     return YELLOW + "💥" + RESET;
            case "JALAPENO":        return RED    + "🌶" + RESET;
            case "CHOMPER":         return MAGENTA+ "👄" + RESET;
            case "MELON_PULT":      return GREEN  + "🍉" + RESET;
            case "WINTER_MELON":    return CYAN   + "🍈" + RESET;
            case "TORCHWOOD":       return RED    + "🔥" + RESET;
            case "SNAPDRAGON":      return RED    + "🐉" + RESET;
            case "LILY_PAD":        return GREEN  + "🍀" + RESET;
            case "MAGNET_SHROOM":   return MAGENTA+ "🧲" + RESET;
            case "HYPNO_SHROOM":    return MAGENTA+ "🍄" + RESET;
            case "LASER_BEAN":      return CYAN   + "💡" + RESET;
            case "PUMPKIN":         return YELLOW + "🎃" + RESET;
            default:                return GREEN  + "🌱" + RESET;
        }
    }

    private String getZombieSymbol(Zombie zombie) {
        if (zombie.isHypnotized()) {
            return GREEN + "Z" + RESET;
        }
        String name = zombie.getType().name();
        switch (name) {
            case "NORMAL":      return RED    + "Z " + RESET;
            case "CONEHEAD":    return RED    + "Zc" + RESET;
            case "BUCKETHEAD":  return RED    + "Zb" + RESET;
            case "KNIGHT":      return RED    + "Zk" + RESET;
            case "GARGANTUAR":  return RED    + BOLD + "G!" + RESET;
            case "IMP":         return RED    + "z " + RESET;
            case "ALL_STAR":    return RED    + "🏈" + RESET;
            case "JESTER_ZOMBIE": return MAGENTA + "♦" + RESET;
            case "WIZARD_ZOMBIE": return MAGENTA + "🧙" + RESET;
            case "KING_ZOMBIE": return MAGENTA + "♚" + RESET;
            case "DRAGON_IMP":  return RED    + "🐲" + RESET;
            case "PROSPECTOR_ZOMBIE": return RED + "💣" + RESET;
            case "PIANST_ZOMBIE": return RED  + "🎹" + RESET;
            default:            return RED    + "Z " + RESET;
        }
    }

    private Zombie getZombieOnTile(int col, int row, GameSession session) {
        double colDouble = col;
        for (Zombie z : session.getActiveZombies()) {
            if (z.getY() == row
                    && Math.abs(z.getX() - colDouble) < 0.5) {
                return z;
            }
        }
        return null;
    }

    private boolean hasSunAt(int col, int row, GameSession session) {
        for (Sun sun : session.getActiveSuns()) {
            if (!sun.isCollected() && sun.isLanded()
                    && sun.getX() == col && sun.getY() == row) {
                return true;
            }
        }
        return false;
    }

    private void printRowSideInfo(int row, GameSession session) {
        boolean mowerAvail = session.getGameMap()
                                    .isLawnMowerAvailable(row - 1);
        String mowerStr = mowerAvail
                          ? GREEN + " 🚜" + RESET
                          : RED   + " ✗ " + RESET;
        System.out.print(mowerStr);
    }

    private void printLawnMowerStatus(GameSession session) {
        System.out.print(CYAN + "Lawn Mowers: " + RESET);
        for (int r = 0; r < session.getGameMap().getRows(); r++) {
            boolean avail = session.getGameMap().isLawnMowerAvailable(r);
            System.out.print("Row " + (r + 1) + ":"
                + (avail ? GREEN + "✔ " + RESET : RED + "✘ " + RESET));
        }
        System.out.println();
    }

    private void printLegend() {
        System.out.println(CYAN + "Legend: "
            + GREEN + "🌻/🌿/🌱=Plants  "
            + RED + "Z=Zombie G!=Garg  "
            + YELLOW + "☀=Sun  "
            + BLUE + "≈=Water  "
            + WHITE + "⛰=Tombstone"
            + RESET);
    }

}
