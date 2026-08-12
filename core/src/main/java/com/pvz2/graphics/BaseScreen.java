package com.pvz2.graphics;

import com.badlogic.gdx.Screen;
import com.pvz2.PVZApplication;
import com.pvz2.model.enums.PlantType;
import java.util.List;

/** کلاس پایه تمام صفحه‌های بازی. */
public abstract class BaseScreen implements Screen {

    protected final PVZApplication game;

    protected BaseScreen(PVZApplication game) { this.game = game; }

    protected void goTo(ScreenId id)        { game.goTo(id); }
    protected GameFacade facade()           { return GameFacade.get(); }

    protected void startGame(String chapter, int level, List<PlantType> plants) {
        game.startGame(chapter, level, plants);
    }

    @Override public void resize(int w, int h) {}
    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}
    @Override public void dispose(){}
}
