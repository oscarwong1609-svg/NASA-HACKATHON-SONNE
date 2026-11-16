package com.missionlab.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.missionlab.model.Mission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads graphics, fonts, sounds and photos.
 * Everything is optional: if a file is missing, the game uses a placeholder.
 * This lets the art team add files without touching the code:
 *   assets/sprites/&lt;part id&gt;.png   (for example cam_sharp.png), and player_0.png ... player_3.png
 *   assets/photos/&lt;mission file&gt;  (for example greenland.png) and off_target.png
 *   assets/sounds/pickup.wav, place.wav, click.wav, deny.wav, launch.wav, success.wav, fail.wav
 * Fonts and pixel art are built into the code ({@link PixelFont}, {@link Sprites}): no files are needed.
 * A PNG in assets/sprites/ named after a part id replaces the built-in drawing.
 */
public final class Assets implements Disposable {
    public final TextureRegion pixel;
    /** Body text (Departure Mono, 7 px per character). */
    public final PixelFont font;
    /** Small text (Tiny5): labels, telemetry, long text. */
    public final PixelFont small;
    /** Titles (Press Start 2P at 2x). */
    public final PixelFont big;
    /** Uppercase labels (Silkscreen): panel headers. */
    public final PixelFont label;
    /** The title-screen logo (Press Start 2P at 4x). */
    public final PixelFont logo;
    /** All of the game's pixel art. */
    public final Sprites sprites;
    /** The intermediate canvas that fills the window without losing sharpness. */
    public final ScreenCanvas canvas = new ScreenCanvas();
    /** The world map (orbit map, ground station desk and Mission Control). */
    public final WorldMap worldMap;

    private final List<Texture> textures = new ArrayList<>();
    private final Map<String, TextureRegion> fileSprites = new HashMap<>();
    private final Map<String, Sound> sounds = new HashMap<>();

    public Assets() {
        Pixmap px = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        px.setColor(Color.WHITE);
        px.fill();
        pixel = new TextureRegion(own(new Texture(px)));
        px.dispose();

        PixelFont[] fonts = PixelFont.buildAll(textures);
        small = fonts[0];
        font = fonts[1];
        big = fonts[2].scaled(2);
        logo = fonts[2].scaled(4);
        label = fonts[3];
        sprites = new Sprites();
        worldMap = new WorldMap();
    }

    private Texture own(Texture t) {
        textures.add(t);
        return t;
    }

    /**
     * A part's sprite: the PNG in assets/sprites/ if the art team added one,
     * otherwise the built-in pixel art (or null if there is none).
     */
    public TextureRegion sprite(String id) {
        if (!fileSprites.containsKey(id)) {
            FileHandle fh = Gdx.files.internal("sprites/" + id + ".png");
            TextureRegion region = null;
            if (fh.exists()) {
                Texture t = own(new Texture(fh));
                t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                region = new TextureRegion(t);
            }
            if (region == null) region = sprites.get(id);
            fileSprites.put(id, region);
        }
        return fileSprites.get(id);
    }

    /** Plays a sound if the file exists; otherwise does nothing. */
    public void play(String name) {
        if (!sounds.containsKey(name)) {
            Sound s = null;
            for (String ext : new String[] {".wav", ".ogg", ".mp3"}) {
                FileHandle fh = Gdx.files.internal("sounds/" + name + ext);
                if (fh.exists()) {
                    s = Gdx.audio.newSound(fh);
                    break;
                }
            }
            sounds.put(name, s);
        }
        Sound s = sounds.get(name);
        if (s != null) s.play(0.6f);
    }

    /**
     * The mission's "real" photo. Reads assets/photos/&lt;file&gt; if it exists;
     * otherwise draws a placeholder image. The caller must dispose() the Pixmap.
     */
    public Pixmap loadPhoto(Mission mission, boolean onTarget) {
        String file = onTarget ? mission.photoFile : "off_target.png";
        FileHandle fh = Gdx.files.internal("photos/" + file);
        if (fh.exists()) return new Pixmap(fh);
        if (!onTarget) return PlaceholderPhotos.ocean();
        return mission.night ? PlaceholderPhotos.nightFires() : PlaceholderPhotos.iceSheet();
    }

    @Override
    public void dispose() {
        worldMap.dispose();
        for (Texture t : textures) t.dispose();
        sprites.dispose();
        canvas.dispose();
        for (Sound s : sounds.values()) if (s != null) s.dispose();
    }
}
