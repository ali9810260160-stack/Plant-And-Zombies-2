package com.pvz2.graphics.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.ObjectMap;
import com.pvz2.model.GameSettings;

/**
 * مدیریتِ مرکزیِ موسیقی و افکت‌های صوتی (FQ2).
 *
 * <p>تمامِ فراخوانی‌های صوتی داخلِ try/catch هستند تا در نبودِ دستگاهِ صوتی
 * (مثلاً محیطِ headless) بازی هرگز کرش نکند. تنظیماتِ روشن/خاموشِ موسیقی و
 * افکت‌ها و بلندیِ صدا از {@link GameSettings} خوانده می‌شود.
 */
public final class SoundManager {

    private static SoundManager instance;

    private Music  music;
    private String musicKey;
    private final ObjectMap<String, Sound> sfxCache = new ObjectMap<>();

    private SoundManager() {}

    public static SoundManager get() {
        if (instance == null) instance = new SoundManager();
        return instance;
    }

    private GameSettings settings() { return GameSettings.getInstance(); }

    // ─── موسیقی ──────────────────────────────────────────────────────────────

    /** پخشِ موسیقیِ پس‌زمینه (loop). اگر همان قطعه در حالِ پخش باشد کاری نمی‌کند. */
    public void playMusic(String path) {
        if (path == null) return;
        if (path.equals(musicKey) && music != null) {
            try { if (music.isPlaying()) return; } catch (Exception ignored) { }
        }
        stopMusic();
        musicKey = path;
        if (!settings().isMusicEnabled()) return;
        music = load(path, true);
    }

    /** پخشِ یک‌بارِ موسیقی (بدونِ loop) — برای صفحه‌ی برد/باخت. */
    public void playMusicOnce(String path) {
        if (path == null) return;
        stopMusic();
        musicKey = path;
        if (!settings().isMusicEnabled()) return;
        music = load(path, false);
    }

    private Music load(String path, boolean loop) {
        try {
            FileHandle fh = Gdx.files.internal(path);
            if (!fh.exists()) {
                Gdx.app.error("SoundManager", "music not found: " + path);
                return null;
            }
            Music m = Gdx.audio.newMusic(fh);
            m.setLooping(loop);
            m.setVolume(settings().getMasterVolume());
            m.play();
            return m;
        } catch (Exception e) {
            Gdx.app.error("SoundManager", "music failed: " + path, e);
            return null;
        }
    }

    public void stopMusic() {
        if (music != null) {
            try { music.stop(); music.dispose(); } catch (Exception ignored) { }
            music = null;
        }
        musicKey = null;
    }

    /** پس از تغییرِ تنظیماتِ صدا (منوی Settings) صدا زده شود. */
    public void applySettings() {
        if (!settings().isMusicEnabled()) {
            stopMusic();
        } else if (music != null) {
            try { music.setVolume(settings().getMasterVolume()); } catch (Exception ignored) { }
        }
    }

    // ─── افکت‌های صوتی ────────────────────────────────────────────────────────

    public void playSfx(String path) {
        if (path == null || !settings().isSfxEnabled()) return;
        try {
            Sound s = sfxCache.get(path);
            if (s == null) {
                FileHandle fh = Gdx.files.internal(path);
                if (!fh.exists()) { Gdx.app.error("SoundManager", "sfx not found: " + path); return; }
                s = Gdx.audio.newSound(fh);
                sfxCache.put(path, s);
            }
            s.play(settings().getMasterVolume());
        } catch (Exception e) {
            Gdx.app.error("SoundManager", "sfx failed: " + path, e);
        }
    }

    public void dispose() {
        stopMusic();
        for (Sound s : sfxCache.values()) {
            try { s.dispose(); } catch (Exception ignored) { }
        }
        sfxCache.clear();
    }

    // ─── کلیدهای asset ────────────────────────────────────────────────────────

    public static final String MUSIC_MENU = "audio/music/menu_theme.mp3";
    public static final String MUSIC_WIN  = "audio/music/win.mp3";
    public static final String MUSIC_LOSS = "audio/music/loss.mp3";
    public static final String MUSIC_BOSS = "audio/music/boss_zomboss.mp3";

    public static final String SFX_EXPLOSION = "audio/sfx/explosion.mp3";
    public static final String SFX_MOWER     = "audio/sfx/mower_trigger.mp3";
    public static final String SFX_WAVE      = "audio/sfx/wave_start.mp3";

    /** انتخابِ موسیقیِ مرحله بر اساسِ نامِ فصل. */
    public static String chapterMusic(String chapter) {
        if (chapter == null) return MUSIC_MENU;
        String c = chapter.toUpperCase();
        if (c.contains("EGYPT"))                     return "audio/music/level_ancient_egypt.mp3";
        if (c.contains("FROST") || c.contains("ICE"))return "audio/music/level_frostbite_caves.mp3";
        if (c.contains("BEACH") || c.contains("WAVE"))return "audio/music/level_big_wave_beach.mp3";
        if (c.contains("DARK"))                      return "audio/music/level_dark_ages.mp3";
        return MUSIC_MENU;
    }
}
