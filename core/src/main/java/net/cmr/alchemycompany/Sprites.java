package net.cmr.alchemycompany;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class Sprites {

    public enum SpriteType {
        SCOUT,
        SOLDIER,
        HIGHLIGHTED_TROOP,

        WATER,
        PLAINS,
        FOREST,
        MOUNTAINS,
        SWAMP,
        CRYSTAL_VALLEY,

        HEADQUARTERS,
        EXTRACTOR,
        STORAGE,
        FACTORY,
        STATUE,
        RESEARCH_LAB,
        ARCHER_TOWER,
        BARRACKS,
        ;

        private final String fileName;
        SpriteType() {
            this.fileName = "sprites/" + name().toLowerCase() + ".png";
        }
        SpriteType(String fileName) {
            this.fileName = fileName;
        }
    }

    private static boolean initialized = false;
    private static Skin skin;
    private static Map<SpriteType, Texture> textures = new HashMap<>();

    public static void load() {
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        skin.get("font", BitmapFont.class).setUseIntegerPositions(false);
        initialized = true;
        textures = new HashMap<>();
        for (SpriteType type : SpriteType.values()) {
            Texture texture = new Texture(Gdx.files.internal(type.fileName));
            textures.put(type, texture);
            System.out.println("Loaded sprite: " + type.name() + " from " + type.fileName);
        }
    }

    public static Skin getSkin() {
        if (!initialized) {
            load();
        }
        return skin;
    }

    public static Texture getTexture(SpriteType type) {
        if (!initialized) {
            load();
        }
        return textures.get(type);
    }

    public static void dispose() {
        if (!initialized) return;
        for (Texture texture : textures.values()) {
            texture.dispose();
        }
        textures.clear();
        skin.dispose();
        initialized = false;
    }

}
