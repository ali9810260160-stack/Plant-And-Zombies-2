package com.pvz2.graphics.actors;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.pvz2.graphics.GameConstants;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.model.enums.PlantType;

import java.util.*;

/**
 * ستونِ عمودیِ کارت‌های گیاه در لبه‌ی چپِ صفحه.
 *
 * <p>مراحلِ عادی: کارت‌های گیاهانِ انتخابی از بالا به پایین چیده می‌شوند (حداکثر
 * {@link #MAX_SLOTS} تا). مراحلِ CONVEYOR_BELT: نوارِ متحرکِ عمودی؛ کارت‌ها از
 * پایین وارد می‌شوند و رو به بالا انباشته می‌شوند.</p>
 *
 * <p>کارت‌ها عمداً پهن‌تر و کوتاه‌تر از حالتِ portraitِ صفحه‌ی انتخاب/کلکسیون‌اند
 * ({@link #V_CARD_W}×{@link #V_CARD_H}) تا هشت‌تا در ارتفاعِ صفحه جا شوند؛ اندازه
 * روی هر کارت با {@code setSize} بازنویسی می‌شود (سازنده‌ی PlantCardActor از
 * getWidth/getHeight می‌کشد، پس ابعادِ سراسریِ CARD_W/H دست‌نخورده می‌ماند).</p>
 */
public class SeedBankActor extends Group {

    public interface OnPlantSelected { void onSelected(PlantType type); }

    // ─── ابعادِ ستونِ عمودی ──────────────────────────────────────────────────────
    public static final float V_CARD_W  = 94f;
    public static final float V_CARD_H  = 62f;
    public static final float V_PAD     = 5f;
    public static final int   MAX_SLOTS = 8;

    private final List<PlantCardActor> cards = new ArrayList<>();
    private PlantCardActor selected;
    private OnPlantSelected callback;
    private final Skin skin;

    // ─── نوارِ متحرکِ عمودی (Conveyor Belt) ──────────────────────────────────────
    private static final String BELT_PATH = "maps/general/convey-belt.png";
    private static final int    BELT_FRAMES = 12;
    private static final float  BELT_FPS = 10f;
    /** ظرفیتِ صفِ نوار (طبقِ داک: حداکثر ۵ گیاه). */
    private static final int    CONVEYOR_CAP = 5;

    private boolean conveyor;
    private boolean showBelt = true;
    private float   beltTime;
    private TextureRegion[] beltFrames;
    private boolean beltLoadTried;

    private static float slotH()   { return V_CARD_H + V_PAD; }
    private static float columnH() { return MAX_SLOTS * slotH(); }

    public SeedBankActor(List<PlantType> plantTypes, Map<PlantType, Integer> costs, Skin skin) {
        this.skin = skin;
        setSize(V_CARD_W, columnH());
        int i = 0;
        for (PlantType type : plantTypes) {
            if (i >= MAX_SLOTS) break;
            int cost = costs.getOrDefault(type, 0);
            PlantCardActor card = new PlantCardActor(type, cost, skin);
            card.setSize(V_CARD_W, V_CARD_H);
            card.setPosition(0, topSlotY(i));
            cards.add(card);
            addActor(card);
            attachClick(card);
            i++;
        }
    }

    /** Yِ محلیِ اسلاتِ i در حالتِ عادی (i=0 بالاترین). */
    private float topSlotY(int i) { return columnH() - (i + 1) * slotH(); }

    // ─── نوارِ کناریِ عمودی ───────────────────────────────────────────────────────

    /**
     * صفِ نوار را با {@code queue} هماهنگ می‌کند بدونِ rebuildِ کامل — گیاهِ جدید از
     * پایین وارد می‌شود و رو به بالا می‌لغزد، گیاهِ کاشته‌شده حذف و بقیه پایین می‌آیند.
     */
    public void syncConveyor(List<PlantType> queue, Map<PlantType, Integer> costs) {
        conveyor = true;
        ensureBeltFrames();

        Map<PlantType, Integer> want = new HashMap<>();
        for (PlantType t : queue) want.merge(t, 1, Integer::sum);

        Map<PlantType, Integer> have = new HashMap<>();
        for (PlantCardActor c : cards) have.merge(c.getPlantType(), 1, Integer::sum);
        for (Map.Entry<PlantType, Integer> e : have.entrySet()) {
            int extra = e.getValue() - want.getOrDefault(e.getKey(), 0);
            while (extra-- > 0) removeOneCardOfType(e.getKey());
        }

        Map<PlantType, Integer> have2 = new HashMap<>();
        for (PlantCardActor c : cards) have2.merge(c.getPlantType(), 1, Integer::sum);
        for (Map.Entry<PlantType, Integer> e : want.entrySet()) {
            int missing = e.getValue() - have2.getOrDefault(e.getKey(), 0);
            while (missing-- > 0) addBeltCard(e.getKey(), costs.getOrDefault(e.getKey(), 0));
        }
    }

    /** سازگاریِ عقب‌رو — همان syncConveyor. */
    public void updateConveyor(List<PlantType> queue, Map<PlantType, Integer> costs) {
        syncConveyor(queue, costs);
    }

    private void removeOneCardOfType(PlantType type) {
        PlantCardActor target = null;
        float minY = Float.MAX_VALUE;                 // پایین‌ترین (قدیمی‌ترینِ در صف)
        for (PlantCardActor c : cards) {
            if (c.getPlantType() == type && c.getY() < minY) { minY = c.getY(); target = c; }
        }
        if (target != null) {
            if (selected == target) { selected = null; if (callback != null) callback.onSelected(null); }
            cards.remove(target);
            target.remove();
        }
    }

    private void addBeltCard(PlantType type, int cost) {
        PlantCardActor card = new PlantCardActor(type, cost, skin);
        card.setSize(V_CARD_W, V_CARD_H);
        card.setPosition(0, -slotH());               // ورود از پایینِ ستون
        cards.add(card);
        addActor(card);
        attachClick(card);
    }

    private void ensureBeltFrames() {
        if (beltFrames != null || beltLoadTried) return;
        beltLoadTried = true;
        TextureRegion full = GameAssets.getInstance().local(BELT_PATH);
        if (full == null || full.getRegionWidth() < BELT_FRAMES) return;
        int fw = full.getRegionWidth() / BELT_FRAMES;
        int fh = full.getRegionHeight();
        beltFrames = new TextureRegion[BELT_FRAMES];
        for (int i = 0; i < BELT_FRAMES; i++) {
            beltFrames[i] = new TextureRegion(full.getTexture(),
                    full.getRegionX() + i * fw, full.getRegionY(), fw, fh);
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (!conveyor) return;
        beltTime += delta;
        // کارت‌ها از پایین (نقطه‌ی ورود) به سمتِ بالا حرکت می‌کنند و نزدیکِ بالای
        // ستون انباشته می‌شوند: قدیمی‌ترین (بالاترین Y) بالای ستون، تازه‌واردها زیرِ آن.
        List<PlantCardActor> ordered = new ArrayList<>(cards);
        ordered.sort(Comparator.<PlantCardActor>comparingDouble(PlantCardActor::getY).reversed());
        for (int i = 0; i < ordered.size(); i++) {
            PlantCardActor c = ordered.get(i);
            float targetY = topSlotY(i);
            float ny = MathUtils.lerp(c.getY(), targetY, Math.min(1f, delta * 7f));
            c.setY(ny);
            c.setX(0);
        }
    }

    /** نمایش/عدم‌نمایشِ پس‌زمینه‌ی نوارِ متحرک (کوزه‌شکنی نوار ندارد). */
    public void setBeltVisible(boolean visible) { this.showBelt = visible; }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (conveyor && showBelt && beltFrames != null) {
            int idx = ((int) (beltTime * BELT_FPS)) % BELT_FRAMES;
            if (idx < 0) idx += BELT_FRAMES;
            float bw = V_CARD_W + 8f;
            batch.setColor(Color.WHITE);
            // نوارِ عمودی، پوشاننده‌ی کلِ ارتفاعِ ستون
            batch.draw(beltFrames[idx], getX() - 4f, getY(), bw, columnH());
        }
        super.draw(batch, parentAlpha);
    }

    private void attachClick(PlantCardActor card) {
        card.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                if (!card.isLocked() && card.isReady()) toggle(card);
            }
        });
    }

    private void toggle(PlantCardActor card) {
        if (selected == card) {
            card.setSelected(false);
            selected = null;
            if (callback != null) callback.onSelected(null);
        } else {
            if (selected != null) selected.setSelected(false);
            selected = card;
            card.setSelected(true);
            if (callback != null) callback.onSelected(card.getPlantType());
        }
    }

    /** بعد از کاشتِ موفق — cooldownِ کارت را شروع کن. */
    public void onPlanted(PlantType type, float cooldownFrac) {
        cards.stream()
                .filter(c -> c.getPlantType() == type)
                .findFirst()
                .ifPresent(c -> { c.setCooldownFraction(cooldownFrac); c.setSelected(false); });
        selected = null;
    }

    /** به‌روزرسانیِ cooldownِ همه‌ی کارت‌ها از داده‌های فاز ۱. */
    public void updateCooldowns(Map<PlantType, Float> fractions) {
        for (PlantCardActor card : cards) {
            Float f = fractions.get(card.getPlantType());
            card.setCooldownFraction(f != null ? f : 0f);
        }
    }

    public void clearSelection() {
        if (selected != null) { selected.setSelected(false); selected = null; }
    }

    public boolean hasSelection()        { return selected != null; }
    public PlantType getSelectedType()   { return selected == null ? null : selected.getPlantType(); }
    public void setOnPlantSelected(OnPlantSelected cb) { callback = cb; }
}
