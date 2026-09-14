package com.pvz2.graphics.screens;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Scaling;
import com.pvz2.graphics.GameConstants;
import com.pvz2.graphics.assets.GameAssets;

/**
 * کمک‌کننده‌ی مشترک برای صفحه‌های احراز هویت (welcome/login/register/recovery/security).
 *
 * <p>همه‌ی این صفحه‌ها یک زبانِ طراحی دارند: یک پس‌زمینه‌ی تمام‌صفحه‌ی از پیش‌ساخته
 * (پوشه‌ی {@code assets/auth/}) که قابِ چوبی + دکوراسیون در آن پخته شده، و روی
 * نواحیِ خالیِ آن ویجت‌های واقعیِ تعاملی (فیلد/دکمه) با موقعیتِ مطلقِ پیکسلی
 * (فضای ۱۲۸۰×۷۲۰، مبدأ پایین-چپ) چیده می‌شوند.</p>
 *
 * <p>تصاویرِ پس‌زمینه ۱۴۰۸×۷۶۸ هستند و روی کلِ ویوپورت کشیده می‌شوند. برای تبدیلِ
 * مختصاتِ داخلِ تصویر به فضای ویوپورت از {@link #ix(float)}/{@link #iy(float)}
 * استفاده کنید (تصویر بالا-چپ‌مبدأ، ویوپورت پایین-چپ‌مبدأ).</p>
 */
final class AuthUi {
    private AuthUi() {}

    static final float VW = GameConstants.VIEWPORT_WIDTH;   // 1280
    static final float VH = GameConstants.VIEWPORT_HEIGHT;  // 720
    static final float IMG_W = 1408f;
    static final float IMG_H = 768f;

    /** پس‌زمینه‌ی تمام‌صفحه از یک فایلِ لوکالِ auth (بدون prefixِ assets/). */
    static Image background(String localPath) {
        Image bg = new Image(GameAssets.getInstance().local(localPath));
        bg.setScaling(Scaling.stretch);
        bg.setBounds(0, 0, VW, VH);
        bg.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
        return bg;
    }

    /** X داخلِ تصویر (۰..۱۴۰۸) → X ویوپورت. */
    static float ix(float imgX) { return imgX / IMG_W * VW; }

    /** Y داخلِ تصویر (۰..۷۶۸ از بالا) → Y ویوپورت (از پایین). */
    static float iy(float imgYFromTop) { return VH - (imgYFromTop / IMG_H * VH); }

    /** عرضِ نسبی داخلِ تصویر → عرضِ ویوپورت. */
    static float iw(float imgW) { return imgW / IMG_W * VW; }

    /** ارتفاعِ نسبی داخلِ تصویر → ارتفاعِ ویوپورت. */
    static float ih(float imgH) { return imgH / IMG_H * VH; }

    /**
     * یک ناحیه‌ی نامرئیِ قابل‌کلیک روی هنرِ pۀخته‌شده (مثلاً پیکانِ برگشت یا دکمه‌ی X).
     * موقعیت/اندازه در فضای ویوپورت.
     */
    static Button hotspot(float x, float y, float w, float h, Runnable onClick) {
        Button b = new Button(new Button.ButtonStyle());   // بدونِ drawable ⇒ نامرئی ولی کلیک‌پذیر
        b.setBounds(x, y, w, h);
        b.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float lx, float ly) { onClick.run(); }
        });
        return b;
    }

    /** قرار دادنِ actor با موقعیت/اندازه‌ی مطلق در فضای ویوپورت. */
    static <T extends Actor> T place(T a, float x, float y, float w, float h) {
        a.setBounds(x, y, w, h);
        return a;
    }
}
