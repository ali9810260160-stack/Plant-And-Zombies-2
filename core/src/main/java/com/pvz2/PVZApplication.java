package com.pvz2;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.pvz2.util.FileUtil;
import com.pvz2.view.CommandDispatcher;

/**
 * نقطه ورود اصلی برنامه.
 * برنامه‌نویسی پیشرفته - دانشگاه صنعتی شریف
 * پروژه: Plants vs. Zombies 2
 */
public class PVZApplication extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture image;

    @Override
    public void create() {
        batch = new SpriteBatch();
        image = new Texture("libgdx.png");

        FileUtil.initDataDirectories();
        CommandDispatcher dispatcher = new CommandDispatcher();
        dispatcher.run();
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        batch.begin();
        batch.draw(image, 140, 210);
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        image.dispose();
    }
}
