package net.cmr.alchemycompany;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.component.RenderComponent;
import net.cmr.alchemycompany.screen.UpdatingImage.UpdatingImageProvider;

public class Sprites {

    private static boolean initialized = false;
    private static Skin skin;
    private static Map<String, Sprite> spritesMap = new HashMap<>();
    private static Map<String, NinePatch> patchMap = new HashMap<>();
    private static Map<String, Animation<TextureRegion>> animationMap = new HashMap<>();

    private static TextureAtlas spriteAtlas, animationAtlas, patchAtlas;

    public static void load() {
        skin = new Skin(Gdx.files.internal("ui/newskin/skin.json"));
        TextureAtlas ta = skin.getAtlas();
        /*ta.getTextures().forEach(t -> {
            t.setFilter(TextureFilter.MipMapNearestNearest, TextureFilter.MipMapNearestNearest);
        });*/
        skin.get("default-font", BitmapFont.class).setUseIntegerPositions(false);
        //skin.get("font", BitmapFont.class).setUseIntegerPositions(false);
        
        initialized = true;

        spriteAtlas = new TextureAtlas(Gdx.files.internal("game_sprites.atlas"));
        animationAtlas = new TextureAtlas(Gdx.files.internal("game_animations.atlas"));
        //patchAtlas = new TextureAtlas(Gdx.files.internal("game_patches.atlas"));

        for (AtlasRegion region : spriteAtlas.getRegions()) {
            Sprite sprite = spriteAtlas.createSprite(region.name);
            spritesMap.put(region.name.toUpperCase(), sprite);
            System.out.println("Loaded sprite: " + region.name.toUpperCase() + " from sprites atlas");
        }

        Json json = new Json();
        FileHandle animationsJson = Gdx.files.internal("texture_info/animations.json");
        FileHandle patchesJson = Gdx.files.internal("texture_info/patches.json");
        JsonValue animationInformation = new JsonReader().parse(animationsJson.readString());
        //JsonValue patchInformation = new JsonReader().parse(patchesJson.readString());

        if (!animationInformation.hasChild("animations") || !animationInformation.getChild("animations").isObject()) {
            throw new RuntimeException("Animation file must have object called \"animations\"");
        }

        for (JsonValue entry = animationInformation.get("animations").child; entry != null; entry = entry.next) {
            String name = entry.name;
            float frameDuration = entry.get("frameDuration").asFloat();
            String playModeString = entry.get("playMode").asString();
            PlayMode playMode = PlayMode.valueOf(playModeString);
            if (frameDuration == 0) {
                frameDuration = 1 / 10f;
            }

            Animation<TextureRegion> animation = new Animation<TextureRegion>(frameDuration, animationAtlas.findRegions(name.toLowerCase()));
            animation.setPlayMode(playMode);
            animationMap.put(name.toUpperCase(), animation);
            System.out.println("Loaded animation: " + name.toUpperCase() + " from animation atlas");
        }

        /*if (!patchInformation.hasChild("patches") || !patchInformation.getChild("patches").isObject()) {
            throw new RuntimeException("Patches file must have object called \"patches\"");
        }

        for (JsonValue entry = patchInformation.get("patches").child; entry != null; entry = entry.next) {
            String name = entry.name;
            int left = entry.getInt("left", 0);
            int right = entry.getInt("right", 0);
            int top = entry.getInt("top", 0);
            int bottom = entry.getInt("bottom", 0);

            NinePatch patch = new NinePatch(patchAtlas.findRegion(name.toLowerCase()), left, right, top, bottom);
            patchMap.put(name.toUpperCase(), patch);
            System.out.println("Loaded patch: " + name.toUpperCase() + " from patch atlas");
        }*/

        /*for (AtlasRegion region : animationAtlas.) {
            JsonValue information = animationInformation.get("animations").get(region.name.toUpperCase());
            float frameDuration = 1 / 10f;
            PlayMode playMode = PlayMode.NORMAL;
            if (information != null) {
                frameDuration = information.getFloat("frameDuration", frameDuration);
                playMode = PlayMode.valueOf(information.getString("playMode", PlayMode.NORMAL.toString()));
            }
            Animation<TextureRegion> animation = new Animation<TextureRegion>(frameDuration, animationAtlas.createSprites(region.name));
            animation.setPlayMode(playMode);
        }*/
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
        return spritesMap.getOrDefault(type.toUpperCase(), spritesMap.get("MISSING_TEXTURE"));
    }

    public static Animation<TextureRegion> getAnimation(String type) {
        if (!initialized) {
            load();
        }
        return animationMap.getOrDefault(type, animationMap.get("MISSING_ANIMATION"));
    }

    public static TextureRegion getTexture(String renderId, RenderType type, float elapsedTime) {
        if (!initialized) {
            load();
        }
        switch (type) {
            case SPRITE:
                return getSprite(renderId);
            case ANIMATION:
                // System.out.println(elapsedTime);
                return getAnimation(renderId).getKeyFrame(elapsedTime);
            default:
                break;
        }
        return null;
    }

    public static Drawable getTextureDrawable(String renderId, RenderType type, float elapsedTime) {
        if (!initialized) {
            load();
        }
        return new TextureRegionDrawable(getTexture(renderId, type, elapsedTime));
    }

    public enum RenderType {
        SPRITE,
        ANIMATION
    }

    public static TextureRegionDrawable getDrawable(String type) {
        return new TextureRegionDrawable(getSprite(type));
    }

    public static void dispose() {
        if (!initialized) return;

        spriteAtlas.dispose();
        animationAtlas.dispose();
        //patchAtlas.dispose();

        spritesMap.clear();
        animationMap.clear();
        patchMap.clear();

        skin.dispose();
        initialized = false;
    }

    public static UpdatingImageProvider createUpdatingImageProvider(RenderComponent rc) {
        return new UpdatingImageProvider() {
            private float elapsedTime = 0;

            @Override
            public Drawable get() {
                if (rc == null) {
                    return getDrawable("MISSING_TEXTURE");
                }
                if (rc.getRenderType() == RenderType.ANIMATION) {
                    elapsedTime += Gdx.graphics.getDeltaTime();
                } else {
                    elapsedTime = 0;
                }
                return getTextureDrawable(rc.renderId, rc.getRenderType(), elapsedTime);
            }
        };
    }

}
