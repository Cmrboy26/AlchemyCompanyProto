package net.cmr.alchemycompany;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class Sprites {

    private static boolean initialized = false;
    private static Skin skin;
    private static Map<String, Sprite> spritesMap = new HashMap<>();
    private static Map<String, NinePatch> patchMap = new HashMap<>();
    private static Map<String, Animation<TextureRegion>> animationMap = new HashMap<>();

    private static TextureAtlas spriteAtlas, animationAtlas, patchAtlas;

    public static void load() {
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        skin.get("font", BitmapFont.class).setUseIntegerPositions(false);
        initialized = true;

        spriteAtlas = new TextureAtlas(Gdx.files.internal("game_sprites.atlas"));
        //animationAtlas = new TextureAtlas(Gdx.files.internal("game_animations.atlas"));
        //patchAtlas = new TextureAtlas(Gdx.files.internal("game_patches.atlas"));

        for (AtlasRegion region : spriteAtlas.getRegions()) {
            Sprite sprite = spriteAtlas.createSprite(region.name);
            spritesMap.put(region.name.toUpperCase(), sprite);
            System.out.println("Loaded sprite: " + region.name + " from sprites atlas");
        }
    }

    public static Skin getSkin() {
        if (!initialized) {
            load();
        }
        return skin;
    }

    public static Sprite getSprite(String type) {
        if (!initialized) {
            load();
        }
        return spritesMap.getOrDefault(type, spritesMap.get("MISSING_TEXTURE"));
    }

    public static TextureRegionDrawable getDrawable(String type) {
        return new TextureRegionDrawable(getSprite(type));
    }

    public static void dispose() {
        if (!initialized) return;

        spriteAtlas.dispose();
        //animationAtlas.dispose();
        //patchAtlas.dispose();

        spritesMap.clear();
        animationMap.clear();
        patchMap.clear();

        skin.dispose();
        initialized = false;
    }

}
