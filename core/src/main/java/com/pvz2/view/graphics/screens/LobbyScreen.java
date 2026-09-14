package com.pvz2.graphics.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.pvz2.PVZApplication;
import com.pvz2.graphics.BaseScreen;
import com.pvz2.graphics.GameConstants;
import com.pvz2.graphics.ScreenId;
import com.pvz2.graphics.actors.NetStatusActor;
import com.pvz2.graphics.assets.AssetIds;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.graphics.net.MultiplayerService;

import pvz.skin.BorderedTable;

import java.util.List;

/**
 * Online multiplayer lobby (I, Zombie 2-player): invite a specific user, join a
 * random-match queue, or browse online players. Incoming challenges and
 * match-found notices are handled globally by {@link MultiplayerService}.
 */
public class LobbyScreen extends BaseScreen {

    private Stage stage;
    private Label status;
    private boolean searching;

    public LobbyScreen(PVZApplication game) { super(game); }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT));
        Gdx.input.setInputProcessor(stage);
        buildUi();
    }

    private void buildUi() {
        Skin skin = GameAssets.getInstance().getSkin();

        if (!AssetIds.MAIN_MENU_BACKGROUND.isEmpty()) {
            Image sky = new Image(GameAssets.getInstance().region(AssetIds.MAIN_MENU_BACKGROUND));
            sky.setScaling(Scaling.fill);
            sky.setFillParent(true);
            sky.setTouchable(Touchable.disabled);
            stage.addActor(sky);
        }

        Table root = new Table();
        root.setFillParent(true);
        root.pad(24).top();

        root.add(new Label("Multiplayer  —  I, Zombie", skin, "big_outline")).padTop(10).padBottom(6).row();

        MultiplayerService mp = MultiplayerService.get();
        if (!mp.isOnline()) {
            root.add(new Label("Server offline — online multiplayer is unavailable.", skin))
                    .padBottom(14).row();
            root.add(new Label("You can still play Couch Play (2 players, 1 PC).", skin))
                    .padBottom(16).row();
            root.add(couchButton(skin)).size(260, 60).padBottom(16).row();
            root.add(backButton(skin)).size(150, 58);
            stage.addActor(root);
            addStatusCorner(skin);
            return;
        }

        // ─── Random match + invite-by-username ────────────────────────────────
        Table actions = new Table();
        TextButton random = new TextButton(searching ? "Cancel Search" : "Random Match", skin,
                searching ? "brown" : "green");
        random.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                if (searching) { mp.cancelQueue(); searching = false; setStatus("Search canceled."); rebuild(); }
                else { String m = mp.queue(); searching = true; setStatus(m); rebuild(); }
            }
        });
        actions.add(random).size(220, 60).padRight(24);

        final TextField target = new TextField("", skin);
        target.setMessageText("opponent username");
        TextButton invite = new TextButton("Invite", skin, "purple");
        invite.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                String u = target.getText().trim();
                if (u.isEmpty()) { setStatus("Enter a username to invite."); return; }
                String err = mp.invite(u);
                setStatus(err == null ? ("Invitation sent to " + u + ".") : err);
            }
        });
        actions.add(new Label("Invite:", skin)).padRight(8);
        actions.add(target).width(220).height(46).padRight(8);
        actions.add(invite).size(120, 50);
        root.add(actions).padBottom(14).row();

        // ─── Online players list ──────────────────────────────────────────────
        BorderedTable panel = new BorderedTable();
        Table list = new Table(skin);
        List<String> online = mp.onlineList();
        if (online.isEmpty()) {
            list.add(new Label("No other players online.", skin)).pad(16).row();
        } else {
            for (String u : online) {
                Table rowT = new Table(skin);
                rowT.add(new Label(u, skin)).left().expandX().padLeft(10);
                TextButton inv = new TextButton("Invite", skin, "green");
                inv.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent e, float x, float y) {
                        String err = mp.invite(u);
                        setStatus(err == null ? ("Invitation sent to " + u + ".") : err);
                    }
                });
                rowT.add(inv).size(110, 44).padRight(8).padTop(4).padBottom(4);
                list.add(rowT).width(560).row();
            }
        }
        ScrollPane scroll = new ScrollPane(list, skin);
        scroll.setFadeScrollBars(false);
        panel.add(scroll).width(600).height(300);
        root.add(panel).padBottom(10).row();

        status = new Label("", skin);
        status.setColor(com.badlogic.gdx.graphics.Color.valueOf("ffe9a8"));
        root.add(status).padBottom(10).row();

        root.add(couchButton(skin)).size(260, 52).padBottom(10).row();

        Table bottom = new Table();
        TextButton refresh = new TextButton("Refresh", skin, "brown");
        refresh.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { rebuild(); }
        });
        bottom.add(refresh).size(140, 56).padRight(20);
        bottom.add(backButton(skin)).size(140, 56);
        root.add(bottom);

        stage.addActor(root);
        addStatusCorner(skin);
    }

    private TextButton couchButton(Skin skin) {
        TextButton b = new TextButton("Couch Play (2P, 1 PC)", skin, "green");
        b.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                if (searching) { MultiplayerService.get().cancelQueue(); searching = false; }
                game.startCouchPlay();
            }
        });
        return b;
    }

    private TextButton backButton(Skin skin) {
        TextButton back = new TextButton("Back", skin, "brown");
        back.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                if (searching) { MultiplayerService.get().cancelQueue(); searching = false; }
                goTo(ScreenId.MAIN_MENU);
            }
        });
        return back;
    }

    private void addStatusCorner(Skin skin) {
        NetStatusActor net = new NetStatusActor(skin);
        net.setPosition(20, 18);
        stage.addActor(net);
    }

    private void setStatus(String s) { if (status != null && s != null) status.setText(s); }

    private void rebuild() { stage.clear(); buildUi(); }

    @Override public void render(float delta) {
        GameAssets.getInstance().update();
        ScreenUtils.clear(0f, 0f, 0f, 1);
        stage.act(delta);
        stage.draw();
    }
    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void dispose() { if (stage != null) stage.dispose(); }
}
