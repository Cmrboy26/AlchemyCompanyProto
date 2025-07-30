package net.cmr.alchemycompany.system;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;

import net.cmr.alchemycompany.IsometricHelper;
import net.cmr.alchemycompany.Sprites;
import net.cmr.alchemycompany.Sprites.SpriteType;
import net.cmr.alchemycompany.component.RenderComponent;
import net.cmr.alchemycompany.component.TilePositionComponent;
import net.cmr.alchemycompany.ecs.Engine;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.ecs.EntitySystem;
import net.cmr.alchemycompany.ecs.Family;
import net.cmr.alchemycompany.world.Tile;
import net.cmr.alchemycompany.world.World;
import net.cmr.alchemycompany.world.World.WorldFeature;

public class RenderSystem extends EntitySystem {
    public static float TILE_SIZE = 128;

    private World world;
    private Family renderFamily;

    public RenderSystem(World world) {
        this.world = world;
        this.renderFamily = Family.all(TilePositionComponent.class, RenderComponent.class);
    }

    public void render(SpriteBatch batch, float delta) {
        int mapWidth = world.width;
        int mapHeight = world.height;

        for (int sum = mapWidth + mapHeight - 2; sum >= 0; sum--) {
            for (int x = 0; x <= sum; x++) {
                int y = sum - x;
                if (x < mapWidth && y < mapHeight) {
                    Vector3 iso = IsometricHelper.project(x, y);
                    boolean invert = isInvert(x, y);
                    Texture texture = getSprite(world.getTile(x, y), x, y);
                    if (texture != null) {
                        batch.draw(texture, (iso.x - (invert ? -1 : 0) - 0.5f)  * TILE_SIZE, (iso.y / 4f - 0.5f) * TILE_SIZE, TILE_SIZE * (invert ? -1 : 1), TILE_SIZE * 1.5f);
                        //batch.draw(texture, iso.x * TILE_SIZE, iso.y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    }
                }
            }
        }
    }

    private boolean isInvert(int x, int y) {
        return (generateNoise(x, y) * 31) % 2 == 1;
    }
    private int generateNoise(int x, int y) {
        return (int) (x * 341873128712L + y * 132897987541L);
    }
    private int getNumberBetween(int x, int y, int min, int max) {
        return new Random(generateNoise(x, y)).nextInt((max- min) + 1) + min;
    }

    private Texture getSprite(Tile tile, int x, int y) {
        WorldFeature feature = tile.getFeature();
        SpriteType spriteType = null;
        switch (feature) {
            case WATER:
                spriteType = SpriteType.WATER;
                break;
            case PLAINS:
                spriteType = SpriteType.PLAINS;
                break;
            case FOREST:
                spriteType = SpriteType.FOREST;
                break;
            case MOUNTAINS:
                spriteType = SpriteType.MOUNTAINS;
                break;
            case CRYSTAL_VALLEY:
                spriteType = SpriteType.CRYSTAL_VALLEY;
                break;
            case SWAMP:
                SpriteType[] options = new SpriteType[] {SpriteType.SWAMP1, SpriteType.SWAMP2, SpriteType.SWAMP3, SpriteType.SWAMP4, SpriteType.SWAMP5};
                int option = getNumberBetween(x, y, 0, 4);
                spriteType = options[option];
                break;
            default:
                break;
        }
        if (spriteType == null) {
            return null;
        }
        return Sprites.getTexture(spriteType);
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
    }

    @Override
    public void removedFromEngine(Engine engine) {
        super.removedFromEngine(engine);
    }

}
