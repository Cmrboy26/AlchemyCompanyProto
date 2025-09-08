package net.cmr.alchemycompany.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;

import net.cmr.alchemycompany.IsometricHelper;
import net.cmr.alchemycompany.Sprites;
import net.cmr.alchemycompany.component.ConstructionComponent;
import net.cmr.alchemycompany.component.RenderComponent;
import net.cmr.alchemycompany.component.TilePositionComponent;
import net.cmr.alchemycompany.ecs.Engine;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.ecs.EntitySystem;
import net.cmr.alchemycompany.ecs.Family;
import net.cmr.alchemycompany.world.Tile;
import net.cmr.alchemycompany.world.TilePoint;
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

    public void render(UUID playerUUID, SpriteBatch batch, float delta) {
        int mapWidth = world.width;
        int mapHeight = world.height;

        // List<Entity> lists = new ArrayList<>(engine.getEntities(renderFamily));
        Set<Entity> renderEntities = engine.getEntities(renderFamily);
        Map<TilePoint, List<Entity>> map = new HashMap<>();
        for (Entity entity : renderEntities) {
            TilePositionComponent tpc = entity.getComponent(TilePositionComponent.class);
            //RenderComponent rc = entity.getComponent(RenderComponent.class);
            TilePoint point = new TilePoint(tpc.tileX, tpc.tileY);
            map.putIfAbsent(point, new ArrayList<>());
            map.get(point).add(entity);
        }
        VisibilitySystem visibilitySystem = engine.getSystem(VisibilitySystem.class);

        for (int sum = mapWidth + mapHeight - 2; sum >= 0; sum--) {
            for (int x = 0; x <= sum; x++) {
                int y = sum - x;
                if (x < mapWidth && y < mapHeight) {
                    TilePoint point = new TilePoint(x, y);
                    if (visibilitySystem != null && !visibilitySystem.wasVisiblePreviously(playerUUID, x, y)) {
                        continue;
                    }
                    boolean isVisibleCurrently = visibilitySystem.isVisibleCurrently(playerUUID, x, y);

                    Vector3 iso = IsometricHelper.project(x, y);
                    boolean invert = isInvert(x, y);
                    Sprite sprite = getSprite(world.getTile(x, y), x, y);

                    if (!isVisibleCurrently) {
                        batch.setColor(Color.GRAY);
                    }

                    if (sprite != null) {
                        SpriteRender render = new SpriteRender(sprite);
                        render.renderSprite(batch, iso, invert, true);
                    }
                    if (!isVisibleCurrently) {
                        batch.setColor(Color.WHITE);
                    } else {
                        List<Entity> currentEntities = map.get(point);
                        if (currentEntities != null) {
                            for (Entity entity : currentEntities) {
                                RenderComponent rc = entity.getComponent(RenderComponent.class);
                                Sprite entitySprite = Sprites.getSprite(rc.spriteType);
                                SpriteRender spriteRender = new SpriteRender(entitySprite);
                                spriteRender.renderSprite(batch, iso, rc.invertable && invert, false);

                                if (entity.hasComponent(ConstructionComponent.class)) {
                                    SpriteRender constructionRender = new SpriteRender("CONSTRUCTION_SCAFFOLD");
                                    constructionRender.renderSprite(batch, iso, rc.invertable && invert, false);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private class SpriteRender {

        private Sprite sprite;

        SpriteRender(Sprite sprite) {
            this.sprite = sprite;
        }

        SpriteRender(String spriteId) {
            this.sprite = Sprites.getSprite(spriteId);
        }

        void renderSprite(SpriteBatch batch, Vector3 iso, boolean invert, boolean tile) {
            float width = sprite.getWidth() * 4f;
            float height = sprite.getHeight() * 4f;
            float displayX = iso.x * TILE_SIZE;
            float displayY = (iso.y + 1) / 4 * TILE_SIZE;
            if (invert) {
                width *= -1;
            }
            displayX -= width / 2;
            if (tile) {
                displayY -= height / 2;
            } else {
                displayY -= TILE_SIZE / 4;
            }
            batch.draw(sprite, displayX, displayY, width, height);
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

    private Sprite getSprite(Tile tile, int x, int y) {
        WorldFeature feature = tile.getFeature();
        String spriteType = null;
        switch (feature) {
            case WATER:
                spriteType = "WATER";
                break;
            case PLAINS:
                spriteType = "PLAINS";
                break;
            case FOREST:
                spriteType = "FOREST";
                break;
            case MOUNTAINS:
                spriteType = "MOUNTAINS";
                break;
            case CRYSTAL_VALLEY:
                spriteType = "CRYSTAL_VALLEY";
                break;
            case SWAMP:
                String[] options = new String[] {"SWAMP1", "SWAMP2", "SWAMP3", "SWAMP4", "SWAMP5"};
                int option = getNumberBetween(x, y, 0, 4);
                spriteType = options[option];
                break;
            default:
                break;
        }
        if (spriteType == null) {
            return null;
        }
        return Sprites.getSprite(spriteType);
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
