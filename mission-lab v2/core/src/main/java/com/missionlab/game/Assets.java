package com.missionlab.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.missionlab.model.Mission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Carga gráficos, fuentes, sonidos y fotos.
 * Todo es opcional: si un archivo no existe, el juego usa un dibujo provisional.
 * Así el equipo de arte puede ir añadiendo archivos sin tocar el código:
 *   assets/sprites/&lt;id de pieza&gt;.png   (por ejemplo cam_sharp.png), y player_0.png ... player_3.png
 *   assets/photos/&lt;archivo del encargo&gt;  (por ejemplo greenland.png) y off_target.png
 *   assets/sounds/pickup.wav, place.wav, click.wav, deny.wav, launch.wav, success.wav, fail.wav
 *   assets/fonts/pixel.fnt              (fuente pixel en formato BMFont, opcional)
 */
public final class Assets implements Disposable {
    public final TextureRegion pixel;
    public final TextureRegion circle;
    public final BitmapFont font;
    public final BitmapFont small;
    public final BitmapFont big;
    /** El mapa del mundo (mapa de órbitas, mesa de estaciones y Mission Control). */
    public final WorldMap worldMap;

    private final List<Texture> textures = new ArrayList<>();
    private final List<BitmapFont> fonts = new ArrayList<>();
    private final Map<String, TextureRegion> sprites = new HashMap<>();
    private final Map<String, Sound> sounds = new HashMap<>();

    public Assets() {
        Pixmap px = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        px.setColor(Color.WHITE);
        px.fill();
        pixel = new TextureRegion(own(new Texture(px)));
        px.dispose();

        Pixmap c = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        c.setColor(Color.WHITE);
        c.fillCircle(32, 32, 31);
        Texture ct = own(new Texture(c));
        ct.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        circle = new TextureRegion(ct);
        c.dispose();

        font = makeFont(1f);
        small = makeFont(0.8f);
        big = makeFont(2f);
        worldMap = new WorldMap();
    }

    private BitmapFont makeFont(float scale) {
        FileHandle custom = Gdx.files.internal("fonts/pixel.fnt");
        BitmapFont f = custom.exists() ? new BitmapFont(custom) : new BitmapFont();
        f.getData().setScale(scale);
        f.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        fonts.add(f);
        return f;
    }

    private Texture own(Texture t) {
        textures.add(t);
        return t;
    }

    /** El sprite de una pieza o de un personaje, o null si todavía no existe el archivo. */
    public TextureRegion sprite(String id) {
        if (!sprites.containsKey(id)) {
            FileHandle fh = Gdx.files.internal("sprites/" + id + ".png");
            TextureRegion region = null;
            if (fh.exists()) {
                Texture t = own(new Texture(fh));
                t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                region = new TextureRegion(t);
            }
            sprites.put(id, region);
        }
        return sprites.get(id);
    }

    /** Toca un sonido si existe el archivo; si no, no hace nada. */
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
     * La foto "real" del encargo. Lee assets/photos/&lt;archivo&gt; si existe;
     * si no, dibuja una imagen provisional. Quien llama debe hacer dispose() del Pixmap.
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
        for (BitmapFont f : fonts) f.dispose();
        for (Sound s : sounds.values()) if (s != null) s.dispose();
    }
}
