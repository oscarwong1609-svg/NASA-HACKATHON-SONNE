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
 * Carga gráficos, fuentes, sonidos y fotos.
 * Todo es opcional: si un archivo no existe, el juego usa un dibujo provisional.
 * Así el equipo de arte puede ir añadiendo archivos sin tocar el código:
 *   assets/sprites/&lt;id de pieza&gt;.png   (por ejemplo cam_sharp.png), y player_0.png ... player_3.png
 *   assets/photos/&lt;archivo del encargo&gt;  (por ejemplo greenland.png) y off_target.png
 *   assets/sounds/pickup.wav, place.wav, click.wav, deny.wav, launch.wav, success.wav, fail.wav
 * Las fuentes y el pixel art vienen dentro del código ({@link PixelFont}, {@link Sprites}): no hace falta ningún archivo.
 * Un PNG en assets/sprites/ con el id de una pieza sustituye al dibujo incluido.
 */
public final class Assets implements Disposable {
    public final TextureRegion pixel;
    /** Texto normal (Departure Mono, 7 px por letra). */
    public final PixelFont font;
    /** Texto pequeño (Tiny5): etiquetas, telemetría, textos largos. */
    public final PixelFont small;
    /** Títulos (Press Start 2P al doble). */
    public final PixelFont big;
    /** Rótulos en mayúsculas (Silkscreen): cabeceras de paneles. */
    public final PixelFont label;
    /** El logo de la pantalla de inicio (Press Start 2P x4). */
    public final PixelFont logo;
    /** Todo el pixel art del juego. */
    public final Sprites sprites;
    /** El mapa del mundo (mapa de órbitas, mesa de estaciones y Mission Control). */
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
     * El sprite de una pieza: el PNG de assets/sprites/ si el equipo de arte lo añadió,
     * si no el pixel art incluido en el juego (o null si no hay ninguno).
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
        sprites.dispose();
        for (Sound s : sounds.values()) if (s != null) s.dispose();
    }
}
