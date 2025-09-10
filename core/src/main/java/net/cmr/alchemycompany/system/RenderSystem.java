package net.cmr.alchemycompany.system;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import net.cmr.alchemycompany.ACEngine;
import net.cmr.alchemycompany.IsometricHelper;
import net.cmr.alchemycompany.Sprites;
import net.cmr.alchemycompany.component.Component;
import net.cmr.alchemycompany.component.ConstructionComponent;
import net.cmr.alchemycompany.component.HoverInfoComponent;
import net.cmr.alchemycompany.component.RenderComponent;
import net.cmr.alchemycompany.component.TilePositionComponent;
import net.cmr.alchemycompany.ecs.Engine;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.ecs.EntitySystem;
import net.cmr.alchemycompany.ecs.Family;
import net.cmr.alchemycompany.screen.GameScreen;
import net.cmr.alchemycompany.world.Tile;
import net.cmr.alchemycompany.world.TilePoint;
import net.cmr.alchemycompany.world.World;
import net.cmr.alchemycompany.world.World.WorldFeature;

public class RenderSystem extends EntitySystem {
    public static float TILE_SIZE = 128;

    private GameScreen screen;
    private World world;
    private Family renderFamily;
    private Stage stage;
    private Skin skin;
    private Map<UUID, Actor> uiDisplayLabelCache;

    public RenderSystem(GameScreen screen) {
        this.screen = screen;
        this.renderFamily = Family.all(TilePositionComponent.class, RenderComponent.class);
        this.stage = new Stage(new ScreenViewport());
        this.skin = Sprites.getSkin();
        this.uiDisplayLabelCache = new HashMap<>();
    }

    @Override
    public void addedToEngine(Engine engine) {
        this.world = engine.as(ACEngine.class).getWorld();
        engine.addEntityChangeListener((entity, added) -> {
            if (added) {
                if (entity.hasComponent(HoverInfoComponent.class)) {
                    HoverInfoComponent hic = entity.getComponent(HoverInfoComponent.class);
                    Table table = createHoverTable(entity, hic);
                    uiDisplayLabelCache.put(entity.getID(), table);
                }
            } else {
                uiDisplayLabelCache.remove(entity.getID());
            }
        });
    }

    @Override
    public void removedFromEngine(Engine engine) {
        super.removedFromEngine(engine);
    }

    public void render(UUID playerUUID, SpriteBatch batch, float delta) {
        int mapWidth = world.width;
        int mapHeight = world.height;

        // List<Entity> lists = new ArrayList<>(engine.getEntities(renderFamily));
        Set<Entity> renderEntities = engine.getEntities(renderFamily);
        Map<TilePoint, List<Entity>> map = new HashMap<>();
        for (Entity entity : renderEntities) {
            TilePositionComponent tpc = entity.getComponent(TilePositionComponent.class);
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

        for (Entity entity : screen.inputHelper.getHoveredEntities()) {
            if (entity.hasComponent(HoverInfoComponent.class) && entity.hasComponent(TilePositionComponent.class)) {
                TilePositionComponent tpc = entity.getComponent(TilePositionComponent.class);
                Vector3 iso = IsometricHelper.project(tpc.tileX, tpc.tileY);

                Vector2 centerPosition = SpriteRender.getTileCenterPosition(iso, 0, 0);
                Actor display = uiDisplayLabelCache.get(entity.getID());
                if (display != null) {
                    float tileOffset = TILE_SIZE / 2;
                    display.setPosition(centerPosition.x, centerPosition.y + tileOffset, Align.bottom);
                    display.draw(batch, 1f);
                }
            }
        }
    }

    private static class SpriteRender {

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
            Vector2 tileCenterPosition = getTileCenterPosition(iso, sprite.getWidth(), sprite.getHeight());
            float displayX = tileCenterPosition.x;
            float displayY =  tileCenterPosition.y;
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

        public static Vector2 getTileCenterPosition(Vector3 iso, float width, float height) {
            width = width * 4f;
            height = height * 4f;
            float displayX = iso.x * TILE_SIZE;
            float displayY = (iso.y + 1) / 4 * TILE_SIZE;
            return new Vector2(displayX, displayY);
        }

    }

    private static Table createHoverTable(Entity entity, HoverInfoComponent hic) {
        /*
            HOVER TEXT is a string that can request certain information from components.
            All next lines will be ignored
            If you want to display a variable from a component, use the following

                $net.cmr.alchemycompany.component.ComponentClassName@variableName
            
            If you want to round the amount of decimal places in something like a float, append.

                &round=2

         */
        Skin skin = Sprites.getSkin();
        Map<String, JsonValue> componentStringMap = new HashMap<>();
        Json json = new Json();
        JsonReader reader = new JsonReader();
        for (Entry<Class<? extends Component>, Component> componentEntry : entity.getComponents().entrySet()) {
            String compJsonString = json.toJson(componentEntry.getValue());
            JsonValue compJsonValue = reader.parse(compJsonString);
            componentStringMap.put(componentEntry.getKey().getName(), compJsonValue);
        }

        String hoverText = hic.hoverText;
        Table table = new Table(Sprites.getSkin());
        try (Scanner inputText = new Scanner(hoverText)) {
            StringBuilder inputBuilder = new StringBuilder();
            while (inputText.hasNext()) {
                String next = inputText.next();
                if (next.charAt(0) == '$') {
                    // Special event, make label with previous and continue
                    if (!inputBuilder.isEmpty()) {
                        Label label = new Label(inputBuilder.toString(), skin);
                        inputBuilder = new StringBuilder();
                        table.add(label).row();
                    }
                    // split 0 will be component class
                    // split 1 will be the variable name
                    // split 2, 3... will be modifiers (rounding, truncating, etc...)
                    String[] split = next.substring(1).split("[@&]");
                    if (split.length < 2) {
                        throw new IOException("Missing essential information for special HoverEvent description with text: "+next);
                    }
                    JsonValue componentJson = componentStringMap.get(split[0]);
                    if (componentJson == null) {
                        // Doesn't have a component of this type, or it's formatted improperly. Either way, don't display it
                        continue;
                    }
                    JsonValue value = componentJson.get(split[1]);
                    if (value == null) {
                        // Value doesn't exist in component. 
                        continue;
                    }
                    // Modifier values
                    int roundAmount = -1;
                    int truncateAmount = -1;

                    for (int i = 2; i < split.length; i++) {
                        String modifierRaw = split[i];
                        String[] modifierTokens = modifierRaw.split("=");
                        if (modifierTokens.length != 2) {
                            throw new IOException("Improperly formatted modifier token: "+modifierRaw);
                        }
                        String modifierKey = modifierTokens[0];
                        String modifierValue = modifierTokens[1];

                        try {
                            switch (modifierKey) {
                                case "round" -> roundAmount = Integer.parseInt(modifierValue);
                                case "truncate" -> truncateAmount = Integer.parseInt(modifierValue);
                                default -> throw new IOException("Modifier "+modifierKey+" not found");
                            }
                        } catch (Exception e) {
                            throw new IOException("Error parsing modifier "+modifierRaw, e);
                        }
                    }

                    String displayText = value.asString();

                    if (roundAmount != -1) {
                        Double doubleValue = null;
                        try {
                            doubleValue = Double.valueOf(displayText);
                        } catch (NumberFormatException nfe) {
                            throw new IOException("Cannot round value "+value.name+" if it is not a double");
                        }
                        StringBuilder rounderFormat = new StringBuilder("#.");
                        for (int i = 0; i < roundAmount; i++) {
                            rounderFormat.append("#");
                        }
                        NumberFormat numberFormat = new DecimalFormat(rounderFormat.toString());
                        displayText = numberFormat.format(doubleValue);
                    }
                    if (truncateAmount != -1) {
                        displayText = displayText.substring(0, Math.min(displayText.length(), truncateAmount));
                    }

                    Label label = new Label(displayText, skin);
                    table.add(label);
                } else {
                    inputBuilder.append(next).append(" ");
                }
            }
            if (!inputBuilder.isEmpty()) {
                Label label = new Label(inputBuilder.toString(), skin);
                inputBuilder = new StringBuilder();
                table.add(label).row();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        return table;
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

}
