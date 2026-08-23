package com.pvz2.graphics.actors;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.pvz2.graphics.GameStateSnapshot;
import com.pvz2.model.enums.ZombieType;

import java.util.ArrayList;
import java.util.List;

/**
 * نوارِ کارت‌های زامبیِ قابل‌کاشت برای مینی‌گیم «من زامبی» — ستونِ عمودی در لبه‌ی
 * چپِ صفحه، دقیقاً مثلِ نوارِ بذرِ گیاهان ({@link SeedBankActor})، تا روندِ کاشتِ
 * زامبی‌ها کاملاً مشابهِ کاشتِ گیاهان باشد.
 *
 * <p>هر کارت ({@link ZombieCardActor}) تصویرِ زامبی + قیمت + overlayِ recharge را
 * نشان می‌دهد. کلیک روی کارت آن را انتخاب می‌کند (کاشت با کلیک روی زمین). کارت‌هایی
 * که خورشیدِ کافی ندارند یا در حالِ recharge هستند تیره می‌شوند و انتخاب نمی‌شوند.
 */
public class ZombieBankActor extends Group {

    public interface OnZombieSelected { void onSelected(ZombieType type); }

    // ابعاد هم‌سبک با ستونِ عمودیِ گیاهان.
    public static final float V_CARD_W = SeedBankActor.V_CARD_W;
    public static final float V_CARD_H = SeedBankActor.V_CARD_H;
    public static final float V_PAD    = SeedBankActor.V_PAD;

    private final List<ZombieCardActor> cards = new ArrayList<>();
    private final List<Boolean> ready = new ArrayList<>();   // آماده برای انتخاب (afford + no cooldown)
    private ZombieCardActor selected;
    private OnZombieSelected callback;

    public ZombieBankActor(List<ZombieType> roster, List<Integer> costs, Skin skin) {
        int n = roster.size();
        float columnH = n * (V_CARD_H + V_PAD);
        setSize(V_CARD_W, columnH);
        for (int i = 0; i < n; i++) {
            final ZombieCardActor card = new ZombieCardActor(roster.get(i), costs.get(i), skin);
            card.setSize(V_CARD_W, V_CARD_H);
            // i=0 بالاترین
            card.setPosition(0, columnH - (i + 1) * (V_CARD_H + V_PAD));
            final int idx = i;
            card.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float sx, float sy) {
                    if (idx < ready.size() && !ready.get(idx)) return; // غیرقابل انتخاب
                    toggle(card);
                }
            });
            addActor(card);
            cards.add(card);
            ready.add(true);
        }
    }

    /** به‌روزرسانیِ قیمت/recharge/در دسترس بودن از snapshot (هر فریم). */
    public void updateCards(List<GameStateSnapshot.ZombieCardInfo> roster) {
        for (int i = 0; i < cards.size() && i < roster.size(); i++) {
            GameStateSnapshot.ZombieCardInfo ci = roster.get(i);
            ZombieCardActor card = cards.get(i);
            card.setSunCost(ci.cost);
            card.setCooldownFraction(ci.cooldownFraction);
            card.setAffordable(ci.affordable);
            boolean ok = ci.affordable && ci.cooldownFraction <= 0f;
            ready.set(i, ok);
            if (!ok && card == selected) clearSelection();
        }
    }

    private void toggle(ZombieCardActor card) {
        if (selected == card) {
            clearSelection();
            if (callback != null) callback.onSelected(null);
            return;
        }
        if (selected != null) selected.setSelected(false);
        selected = card;
        card.setSelected(true);
        if (callback != null) callback.onSelected(card.getZombieType());
    }

    public void clearSelection() {
        if (selected != null) { selected.setSelected(false); selected = null; }
    }

    public void setOnZombieSelected(OnZombieSelected cb) { this.callback = cb; }
}
