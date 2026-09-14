package com.pvz2.graphics.actors;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.pvz2.graphics.net.NetClient;

/**
 * A tiny self-refreshing label showing the server connection state, so players
 * can tell online (server-backed) from offline (local fallback) at a glance.
 * Drop it onto any stage; it updates its own text/color each frame.
 */
public class NetStatusActor extends Label {

    private static final Color ONLINE = new Color(0.36f, 0.85f, 0.40f, 1f);
    private static final Color OFFLINE = new Color(0.72f, 0.72f, 0.72f, 1f);

    public NetStatusActor(Skin skin) {
        super("", skin);
        setColor(OFFLINE);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        NetClient n = NetClient.get();
        if (n.isConnected()) {
            setText("● Online — " + n.serverName());
            setColor(ONLINE);
        } else {
            setText("○ Offline (local)");
            setColor(OFFLINE);
        }
    }
}
