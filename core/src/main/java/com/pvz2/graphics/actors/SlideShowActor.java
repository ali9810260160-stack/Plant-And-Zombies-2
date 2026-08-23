package com.pvz2.graphics.actors;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.pvz2.graphics.assets.GameAssets;

/**
 * اسلایدشوی تصاویر با دایره‌های ناوبریِ زیرِ آن. هر {@value #AUTO} ثانیه به‌طور
 * خودکار به تصویرِ بعدی می‌رود؛ با کلیک روی هر دایره می‌توان مستقیماً به آن
 * اسلاید رفت. برای منوی اصلی (زیرِ لوگو) استفاده می‌شود.
 */
public class SlideShowActor extends Table {

    private static final float AUTO = 4.5f;   // فاصله‌ی جابه‌جاییِ خودکار (ثانیه)
    private static final Color DOT_ON  = new Color(1f, 0.85f, 0.25f, 1f);
    private static final Color DOT_OFF = new Color(1f, 1f, 1f, 0.45f);

    private final TextureRegion[] slides;
    private final Image slideImage;
    private final Array<Image> dots = new Array<>();
    private int index;
    private float timer;

    public SlideShowActor(String[] localPaths, float imgW, float imgH) {
        GameAssets a = GameAssets.getInstance();
        slides = new TextureRegion[localPaths.length];
        for (int i = 0; i < localPaths.length; i++) slides[i] = a.local(localPaths[i]);

        slideImage = new Image(slides.length > 0 ? slides[0] : a.getWhiteRegion());
        slideImage.setScaling(Scaling.fit);
        add(slideImage).size(imgW, imgH).row();

        Table dotRow = new Table();
        TextureRegion disc = a.getDiscRegion();
        for (int i = 0; i < slides.length; i++) {
            final int fi = i;
            Image dot = new Image(new TextureRegionDrawable(disc));
            dot.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float x, float y) { setIndex(fi); }
            });
            Container<Image> c = new Container<>(dot);
            c.size(16f).pad(5f);
            dotRow.add(c);
            dots.add(dot);
        }
        add(dotRow).padTop(6f);
        updateDots();
    }

    private void setIndex(int i) {
        if (slides.length == 0) return;
        index = ((i % slides.length) + slides.length) % slides.length;
        timer = 0f;
        slideImage.setDrawable(new TextureRegionDrawable(slides[index]));
        updateDots();
    }

    private void updateDots() {
        for (int i = 0; i < dots.size; i++) dots.get(i).setColor(i == index ? DOT_ON : DOT_OFF);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (slides.length <= 1) return;
        timer += delta;
        if (timer >= AUTO) setIndex(index + 1);
    }
}
