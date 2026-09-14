package com.pvz2.graphics.actors;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
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

import java.util.List;

import pvz.libpvz.pam.PamPlayer;

/**
 * دیالوگِ ان‌پی‌سی (BX2/BX3). در ابتدای مرحله دو ان‌پی‌سی — «Crazy Dave» و ماشینِ
 * سخنگویش «Winnie» — چند خط گفت‌وگو رد و بدل می‌کنند. با هر کلیک، خطِ بعد نمایش
 * داده می‌شود؛ در پایان، {@code onComplete} صدا زده می‌شود.
 *
 * <p>پرتره‌ها با PAMهای واقعیِ بازی رندر می‌شوند (اگر asset در دسترس باشد)، وگرنه
 * یک بُستِ رنگیِ جایگزین رسم می‌شود تا هیچ‌وقت خالی نماند.
 */
public class NpcDialogOverlay extends Group {

    public interface Listener { void onComplete(); }

    /** یک خطِ گفت‌وگو. */
    public static final class Line {
        public final boolean dave;   // true=Crazy Dave، false=Winnie
        public final String  text;
        public Line(boolean dave, String text) { this.dave = dave; this.text = text; }
    }

    private static final String DAVE_PAM   = "768/INITIAL/CRAZYDAVE/CRAZYDAVE/CRAZYDAVE.PAM";
    private static final String WINNIE_PAM = "768/INITIAL/WINNIE/WINNIE/WINNIE.PAM";
    private static final String CLIP_IDLE  = "anim_idle";
    private static final String CLIP_TALK  = "anim_mediumtalk";

    private final List<Line> script;
    private final Listener   listener;
    private int index;
    private float time;

    private Label nameLabel;
    private Label textLabel;

    public NpcDialogOverlay(Skin skin, List<Line> script, Listener listener) {
        this.script   = script;
        this.listener = listener;
        setSize(GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT);

        // ─── تیره‌کننده‌ی نیمه‌شفاف؛ کلیک روی آن = خطِ بعد ───
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();
        Image dim = new Image(new TextureRegionDrawable(white));
        dim.setColor(0f, 0f, 0f, 0.45f);
        dim.setSize(getWidth(), getHeight());
        dim.addListener(new InputListener() {
            @Override public boolean touchDown(InputEvent e, float x, float y,
                                               int p, int b) { advance(); return true; }
        });
        addActor(dim);

        // ─── پرتره‌ها (رسمِ دستیِ PAM) ───
        addActor(new PortraitActor());

        // ─── پنلِ گفت‌وگو در پایین ───
        Table panel = new Table(skin);
        try { panel.setBackground("image_ui_dialog_asset_inner_bkgd_10"); }
        catch (Exception ignored) { }
        panel.pad(20, 28, 20, 28);
        panel.setSize(980, 150);
        panel.setPosition((getWidth() - 980) / 2f, 24);

        nameLabel = new Label("", skin, "big");
        nameLabel.setColor(Color.YELLOW);
        panel.add(nameLabel).left().padBottom(6).row();

        textLabel = new Label("", skin);
        textLabel.setWrap(true);
        panel.add(textLabel).width(900).left().expandX().row();

        Table bottom = new Table(skin);
        TextButton next = new TextButton("Next  ▶", skin, "green");
        next.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { advance(); }
        });
        TextButton skip = new TextButton("Skip", skin);
        skip.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { finish(); }
        });
        bottom.add(skip).width(120).height(44).padRight(12);
        bottom.add(next).width(160).height(44);
        panel.add(bottom).right().padTop(8);

        addActor(panel);

        showLine();
    }

    private void advance() {
        index++;
        if (index >= script.size()) { finish(); return; }
        time = 0f;
        showLine();
    }

    private void showLine() {
        if (index < 0 || index >= script.size()) return;
        Line l = script.get(index);
        nameLabel.setText(l.dave ? "Crazy Dave" : "Winnie");
        textLabel.setText(l.text);
    }

    private void finish() {
        remove();
        if (listener != null) listener.onComplete();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        time += delta;
    }

    public void show(Stage stage) { stage.addActor(this); }

    private boolean currentIsDave() {
        return index >= 0 && index < script.size() && script.get(index).dave;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Portrait — رندرِ مستقیمِ PAM با بُستِ جایگزین
    // ─────────────────────────────────────────────────────────────────────────

    private final class PortraitActor extends Actor {

        // جعبه‌ی هر پرتره در فضای صفحه (پایین-چپ = Dave، پایین-راست = Winnie)
        private static final float BOX_W = 300f, BOX_H = 300f, BOX_Y = 150f;

        @Override
        public void draw(Batch batch, float parentAlpha) {
            boolean daveActive = currentIsDave();
            // Dave سمت چپ
            drawNpc(batch, DAVE_PAM, daveActive ? CLIP_TALK : CLIP_IDLE,
                    40f, BOX_Y, BOX_W, BOX_H, new Color(0.85f, 0.66f, 0.45f, 1f));
            // Winnie سمت راست
            drawNpc(batch, WINNIE_PAM, !daveActive ? CLIP_TALK : CLIP_IDLE,
                    GameConstants.VIEWPORT_WIDTH - BOX_W - 40f, BOX_Y, BOX_W, BOX_H,
                    new Color(0.45f, 0.62f, 0.85f, 1f));
        }

        /** پرترهٔ یک ان‌پی‌سی را در جعبهٔ داده‌شده جا می‌دهد؛ در صورت نبودِ asset، بُستِ رنگی. */
        private void drawNpc(Batch batch, String pam, String clip, float boxX, float boxY,
                             float boxW, float boxH, Color fallback) {
            GameAssets assets = GameAssets.getInstance();
            if (assets.hasPvzAssets()) {
                PamPlayer p = assets.getPamPlayer();
                if (p != null && drawPam(batch, p, pam, clip, boxX, boxY, boxW, boxH)) return;
            }
            drawFallbackBust(batch, boxX, boxY, boxW, boxH, fallback);
        }

        private boolean drawPam(Batch batch, PamPlayer p, String pam, String clip,
                                float boxX, float boxY, float boxW, float boxH) {
            // اگر PAM هنوز bake نشده یا clip نامعتبر است → بُستِ جایگزین
            try { if (p.getClip(pam, clip) == null) return false; } catch (Exception e) { return false; }

            // PamPlayer کلِ canvas را حولِ نقطه‌ی (x,y) که می‌دهیم مرکز می‌کند. پس
            // canvas را با ماتریسِ transform حولِ مرکزِ جعبه scale می‌کنیم و همان‌جا
            // مرکز رسم می‌کنیم — بدونِ عدمِ‌تطابقِ مبدأ (که باعثِ «ردِ» ناقص می‌شد).
            Rectangle canvas;
            try { canvas = p.bounds(pam); } catch (Exception e) { return false; }
            if (canvas == null || canvas.width <= 0 || canvas.height <= 0) return false;

            float s = Math.min(boxW / canvas.width, boxH / canvas.height);
            float cx = boxX + boxW / 2f;
            float cy = boxY + boxH / 2f;

            // ⚠️ اکتورِ dim (تصویرِ تیره) رنگِ batch را روی سیاهِ نیمه‌شفاف رها می‌کند و
            // Image.draw ی libGDX آن را بازنشانی نمی‌کند؛ اگر رنگ را به سفید برنگردانیم،
            // PamPlayer پرتره را در همان سیاهِ ۴۵٪ ضرب می‌کند و پرتره نامرئی می‌شود.
            Color savedColor = batch.getColor().cpy();
            batch.setColor(Color.WHITE);
            Matrix4 old = batch.getTransformMatrix().cpy();
            batch.setTransformMatrix(old.cpy()
                    .translate(cx, cy, 0f).scale(s, s, 1f).translate(-cx, -cy, 0f));
            try {
                p.draw(batch, pam, clip, time, cx, cy, true);
            } catch (Exception e) {
                batch.setTransformMatrix(old);
                batch.setColor(savedColor);
                return false;
            }
            batch.setTransformMatrix(old);
            batch.setColor(savedColor);
            return true;
        }

        private void drawFallbackBust(Batch batch, float boxX, float boxY,
                                      float boxW, float boxH, Color col) {
            TextureRegion disc = GameAssets.getInstance().getDiscRegion();
            float cx = boxX + boxW / 2f;
            float headR = boxW * 0.32f;
            float headCy = boxY + boxH * 0.62f;
            // شانه/بدنه
            batch.setColor(col.r * 0.8f, col.g * 0.8f, col.b * 0.8f, 1f);
            batch.draw(disc, cx - boxW * 0.34f, boxY + boxH * 0.12f, boxW * 0.68f, boxH * 0.44f);
            // سر
            batch.setColor(col);
            batch.draw(disc, cx - headR, headCy - headR, headR * 2f, headR * 2f);
            batch.setColor(Color.WHITE);
        }
    }
}
