package com.pvz2.graphics.actors;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.pvz2.graphics.GameConstants;
import com.pvz2.model.enums.PlantType;

import java.util.*;

/**
 * نوار کارت‌های گیاه در پایین صفحه.
 *
 * <p>برای مراحل عادی: کارت‌های گیاهان انتخابی
 * برای CONVEYOR_BELT: نمایش صف نوار کناری
 */
public class SeedBankActor extends Group {

    public interface OnPlantSelected { void onSelected(PlantType type); }

    private final List<PlantCardActor> cards = new ArrayList<>();
    private PlantCardActor selected;
    private OnPlantSelected callback;
    private final Skin skin;

    public SeedBankActor(List<PlantType> plantTypes, Map<PlantType, Integer> costs,
                          Skin skin) {
        this.skin = skin;
        float xOff = 4f;
        for (PlantType type : plantTypes) {
            int cost = costs.getOrDefault(type, 0);
            addCard(new PlantCardActor(type, cost, skin), xOff);
            xOff += GameConstants.CARD_W + GameConstants.CARD_PAD;
        }
        setSize(xOff, GameConstants.SB_H);
    }

    /** به‌روزرسانی صف Conveyor Belt */
    public void updateConveyor(List<PlantType> queue, Map<PlantType, Integer> costs) {
        clearChildren();
        cards.clear();
        selected = null;
        float xOff = 4f;
        for (PlantType type : queue) {
            int cost = costs.getOrDefault(type, 0);
            addCard(new PlantCardActor(type, cost, skin), xOff);
            xOff += GameConstants.CARD_W + GameConstants.CARD_PAD;
        }
    }

    private void addCard(PlantCardActor card, float xOff) {
        card.setPosition(xOff, (GameConstants.SB_H - GameConstants.CARD_H) * 0.5f);
        cards.add(card);
        addActor(card);
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

    /** بعد از کاشت موفق — cooldown کارت را شروع کن */
    public void onPlanted(PlantType type, float cooldownFrac) {
        cards.stream()
                .filter(c -> c.getPlantType() == type)
                .findFirst()
                .ifPresent(c -> {
                    c.setCooldownFraction(cooldownFrac);
                    c.setSelected(false);
                });
        selected = null;
    }

    /** به‌روزرسانی cooldown همه کارت‌ها از داده‌های فاز ۱ */
    public void updateCooldowns(Map<PlantType, Float> fractions) {
        for (PlantCardActor card : cards) {
            Float f = fractions.get(card.getPlantType());
            if (f != null) card.setCooldownFraction(f);
        }
    }

    public void clearSelection() {
        if (selected != null) { selected.setSelected(false); selected = null; }
    }

    public boolean hasSelection()        { return selected != null; }
    public PlantType getSelectedType()   { return selected == null ? null : selected.getPlantType(); }
    public void setOnPlantSelected(OnPlantSelected cb) { callback = cb; }
}
