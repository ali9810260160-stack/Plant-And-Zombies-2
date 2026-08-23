package com.pvz2.graphics.actors;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.pvz2.graphics.GameConstants;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.model.enums.LevelType;

/**
 * دیالوگِ آغازِ مرحله (BJ2). در ابتدای هر مرحله نمایش داده می‌شود و مأموریتِ آن
 * مرحله را بر اساسِ نوعِ مرحله شرح می‌دهد (BJ3/BJ5). با فشردنِ دکمه، بازی آغاز
 * می‌شود.
 */
public class LevelStartDialog extends Table {

    public interface Listener { void onStart(); }

    public LevelStartDialog(Skin skin, LevelType type, String chapter,
                            int levelNumber, Listener l) {
        super(skin);
        setFillParent(true);

        // لایه‌ی تیره‌کننده‌ی نیمه‌شفاف روی کلِ صفحه
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();
        Image dim = new Image(new TextureRegionDrawable(white));
        dim.setColor(0f, 0f, 0f, 0.5f);
        dim.setFillParent(true);
        // جذبِ کلیک‌ها تا حین نمایشِ دیالوگ، کاشت روی شبکه انجام نشود
        dim.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override public boolean touchDown(InputEvent e, float x, float y,
                                               int pointer, int button) { return true; }
        });
        addActor(dim);

        Table panel = new Table(skin);
        try { panel.setBackground("image_ui_dialog_asset_inner_bkgd_10"); }
        catch (Exception ignored) { }
        panel.pad(30);

        Label header = new Label(headerText(type, chapter, levelNumber), skin, "big");
        header.setColor(Color.YELLOW);
        header.setAlignment(1);
        panel.add(header).padBottom(10).width(560).row();

        Label objTitle = new Label("MISSION", skin);
        objTitle.setColor(new Color(0.6f, 1f, 0.5f, 1f));
        panel.add(objTitle).padBottom(6).row();

        Label obj = new Label(objectiveText(type), skin);
        obj.setWrap(true);
        obj.setAlignment(1);
        panel.add(obj).width(540).padBottom(24).row();

        TextButton start = new TextButton("Let's Go!", skin, "green");
        start.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { l.onStart(); }
        });
        panel.add(start).width(240).height(56);

        Table center = new Table(skin);
        center.setFillParent(true);
        center.add(panel).center();
        addActor(center);
    }

    private String headerText(LevelType type, String chapter, int levelNumber) {
        String ch = chapter == null ? "" : prettify(chapter);
        switch (type) {
            case VASEBREAKER:     return "Vasebreaker";
            case WALLNUT_BOWLING: return "Wall-nut Bowling";
            case I_ZOMBIE:        return "I, Zombie";
            case BEGHOULED:       return "Beghouled";
            case ZOMBOTANY:       return "Zombotany";
            case SCORED:          return "Scored Game";
            case BOSS:            return ch + " — Boss Battle";
            default:              return ch + " — Level " + levelNumber;
        }
    }

    private String objectiveText(LevelType type) {
        switch (type) {
            case NORMAL:
                return "Don't let the zombies reach your house!";
            case BOSS:
                return "Defeat the Zomboss! Survive its attacks and destroy it "
                        + "before it breaks through.";
            case CONVEYOR_BELT:
                return "Plants arrive on a conveyor belt — plant whatever you "
                        + "receive and hold the line!";
            case LOCKED_PLANTS:
                return "Some of your plants are locked. Make do with what's "
                        + "available and stop the zombies.";
            case SAVE_OUR_SEEDS:
                return "Protect the marked endangered plants — if a zombie eats "
                        + "one of them, you lose!";
            case TIMED_WAR:
                return "Reach the target before the timer runs out!";
            case NIGHT_OPS:
                return "No sun falls from the night sky — rely on mushrooms and "
                        + "sun-producing plants.";
            case DEAD_LINE:
                return "Never let a single zombie cross the dead line!";
            case LOVE_YOUR_PLANTS:
                return "Don't lose more than the allowed number of plants!";
            case PLANT_WHAT_YOU_GET:
                return "You begin with a fixed amount of sun — spend it wisely, "
                        + "there's no more coming.";
            case VASEBREAKER:
                return "Break every vase! Some hide zombies, others hide plants "
                        + "you can use. Clear them all.";
            case WALLNUT_BOWLING:
                return "Bowl wall-nuts down the lanes to crush the zombies before "
                        + "they reach your house!";
            case I_ZOMBIE:
                return "Play as the zombies! Spend sun to place zombies and eat "
                        + "all the brains.";
            case BEGHOULED:
                return "Swap adjacent plants to match three or more and clear the "
                        + "board.";
            case ZOMBOTANY:
                return "Plant-zombie hybrids are on the attack — defend your "
                        + "house against them!";
            case SCORED:
                return "Earn MeoPoints:\n"
                        + "- Speed-Kill Combo: kill 3 zombies in quick succession\n"
                        + "- AoE Wipe: destroy 3+ zombies at the same time\n"
                        + "- Item Collector: collect 5 suns within a short window\n"
                        + "Aim for the highest MeoPoint score!";
            default:
                return "Don't let the zombies reach your house!";
        }
    }

    /** «EGYPT» → «Egypt»، «BIG_WAVE_BEACH» → «Big Wave Beach». */
    private String prettify(String raw) {
        String[] parts = raw.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)))
              .append(p.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    public void show(Stage stage) {
        setSize(GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT);
        stage.addActor(this);
    }
}
