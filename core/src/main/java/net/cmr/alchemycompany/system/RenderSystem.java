package net.cmr.alchemycompany.system;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.ACEngine;
import net.cmr.alchemycompany.IsometricHelper;
import net.cmr.alchemycompany.Sprites;
import net.cmr.alchemycompany.Sprites.RenderType;
import net.cmr.alchemycompany.component.Component;
import net.cmr.alchemycompany.component.ConstructionComponent;
import net.cmr.alchemycompany.component.HoverInfoComponent;
import net.cmr.alchemycompany.component.MovementPathComponent;
import net.cmr.alchemycompany.component.RenderComponent;
import net.cmr.alchemycompany.component.TilePositionComponent;
import net.cmr.alchemycompany.ecs.Engine;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.ecs.EntitySystem;
import net.cmr.alchemycompany.ecs.Family;
import net.cmr.alchemycompany.network.packet.EntityPacket.EntityState;
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
    private Map<UUID, Table> uiDisplayLabelCache;

    public RenderSystem(GameScreen screen) {
        this.screen = screen;
        this.renderFamily = Family.all(TilePositionComponent.class, RenderComponent.class);
        this.uiDisplayLabelCache = new HashMap<>();
    }

    @Override
    public void addedToEngine(Engine engine) {
        this.world = engine.as(ACEngine.class).getWorld();
        engine.addEntityChangeListener((entity, state) -> {
            if (state == EntityState.ADDED) {
                if (entity.hasComponent(HoverInfoComponent.class)) {
                    HoverInfoComponent hic = entity.getComponent(HoverInfoComponent.class);
                    Table table = createHoverTable(entity, hic, this);
                    uiDisplayLabelCache.put(entity.getID(), table);
                }
            } else {
                uiDisplayLabelCache.remove(entity.getID());
            }
        });
        engine.addComponentChangeListener((entity, componentClass) -> {
            if (entity == null) return;
            if (entity.hasComponent(HoverInfoComponent.class)) {
                HoverInfoComponent hic = entity.getComponent(HoverInfoComponent.class);
                Table table = createHoverTable(entity, hic, this);
                uiDisplayLabelCache.put(entity.getID(), table);
            }
        });
    }

    @Override
    public void removedFromEngine(Engine engine) {
        super.removedFromEngine(engine);
    }

    float tileElapsedTime = 0;
    public void render(UUID playerUUID, SpriteBatch batch, float delta) {
        tileElapsedTime += delta;
        List<SpriteRender> tileRenders = renderTiles(playerUUID);
        List<SpriteRender> entityRenders = renderEntities(delta);
        List<SpriteRender> allRenders = new ArrayList<>();
        allRenders.addAll(tileRenders);
        allRenders.addAll(entityRenders);
        Collections.sort(allRenders, Comparator.comparingDouble(SpriteRender::getZ));
        for (SpriteRender render : allRenders) {
            render.render(batch);
        }
        renderHoverUI(batch, delta);
    }

    private List<SpriteRender> renderTiles(UUID playerUUID) {
        int mapWidth = world.width;
        int mapHeight = world.height;
        VisibilitySystem visibilitySystem = engine.getSystem(VisibilitySystem.class);
        List<SpriteRender> tileRenders = new ArrayList<>();
        for (int sum = mapWidth + mapHeight - 2; sum >= 0; sum--) {
            for (int x = 0; x <= sum; x++) {
                int y = sum - x;
                if (x < mapWidth && y < mapHeight) {
                    if (visibilitySystem != null && !visibilitySystem.wasVisiblePreviously(playerUUID, x, y)) {
                        continue;
                    }
                    boolean isVisibleCurrently = visibilitySystem.isVisibleCurrently(playerUUID, x, y);
                    TilePoint tp = new TilePoint(x, y);
                    String spriteId = getSprite(world.getTile(x, y), x, y);
                    Vector2 isoPosition = SpriteRender.calculateSpriteCenter(tp.toVector());
                    isoPosition.add(0, -2);
                    isoPosition.scl(1, 1 / 4f);
                    SpriteRender tileRender = new SpriteRender(spriteId, RenderType.SPRITE, isoPosition, false, tileElapsedTime);
                    if (!isVisibleCurrently) {
                        tileRender.setColor(Color.GRAY);
                    }
                    tileRenders.add(tileRender);
                }
            }
        }
        return tileRenders;
    }

    private List<SpriteRender> renderEntities(float delta) {
        Set<Entity> renderEntities = engine.getEntities(renderFamily);
        List<SpriteRender> entityRenders = new ArrayList<>();
        VisibilitySystem visibilitySystem = engine.getSystem(VisibilitySystem.class);
        for (Entity entity : renderEntities) {
            TilePositionComponent tpc = entity.getComponent(TilePositionComponent.class);
            TilePoint tp = new TilePoint(tpc.tileX, tpc.tileY);
            if (visibilitySystem != null && !visibilitySystem.wasVisiblePreviously(screen.getPlayerUUID(), tpc.tileX, tpc.tileY)) {
                continue;
            }
            if (entity.hasComponent(MovementPathComponent.class)) {
                if (entity.getComponent(MovementPathComponent.class).isFinished(2)) {
                    entity.removeComponent(MovementPathComponent.class, engine);
                } else {
                    tp = entity.getComponent(MovementPathComponent.class).getVisiualPosition(2f);
                    tp.setX(tp.getX());
                    tp.setY(tp.getY());
                }
            }
            entity.getComponent(RenderComponent.class).elapsedTime += delta;
            Vector2 tpVector = tp.toVector();
            if (entity.hasComponent(MovementPathComponent.class)) {
                tpVector.add(entity.getComponent(MovementPathComponent.class).getOffsetVisualPosition(2));
            }
            
            Vector2 isoPosition = SpriteRender.calculateSpriteCenter(tpVector);
            isoPosition.scl(1, 1 / 4f);
            RenderComponent rc = entity.getComponent(RenderComponent.class);
            String currentRenderId = rc.renderId;
            RenderType renderType = rc.getRenderType();
            if (rc.variants != null && rc.variants.size() > 0) {
                Map<Class<? extends Component>, String> variantMap = new LinkedHashMap<>();
                variantMap.put(MovementPathComponent.class, "moving");
                variantMap.put(null, "idle");
                for (Entry<Class<? extends Component>, String> entry : variantMap.entrySet()) {
                    if (entry.getKey() == null || entity.hasComponent(entry.getKey())) {
                        String componentVariant = entry.getValue();
                        String renderComponentVariantId = rc.variants.get(componentVariant);
                        if (renderComponentVariantId != null) {
                            if (renderComponentVariantId.equals(currentRenderId)) {
                                break;
                            } else {
                                rc.renderId = renderComponentVariantId;
                                rc.elapsedTime = 0;
                                break;
                            }
                        }
                    }
                }
            }
            SpriteRender entityRender = new SpriteRender(currentRenderId, renderType, isoPosition, false, rc.elapsedTime);
            entityRender.zOffset = -1 / 2f;
            if (entity.hasComponent(MovementPathComponent.class)) {
                entityRender.zOffset = -1 / 1.5f;
            }
            entityRenders.add(entityRender);
            if (entity.hasComponent(ConstructionComponent.class)) {
                SpriteRender constructionScaffold = new SpriteRender("CONSTRUCTION_SCAFFOLD", RenderType.SPRITE, isoPosition.cpy(), false, rc.elapsedTime);
                constructionScaffold.zOffset = -1 / 2f;
                entityRenders.add(constructionScaffold);
            }
        }
        return entityRenders;
    }

    private void renderHoverUI(SpriteBatch batch, float delta) {
        SelectionSystem ss = engine.getSystem(SelectionSystem.class);
        List<Entity> hoveredEntities = screen.inputHelper.getHoveredEntities(false);
        hoveredEntities.removeIf((entity) -> {
            return !(entity.hasComponent(HoverInfoComponent.class) && entity.hasComponent(TilePositionComponent.class));
        });
        int selectedEntityIndex = hoveredEntities.indexOf(engine.getEntity(ss.getSelectedId()));
        Set<Entity> renderHoverEntities = new HashSet<>();
        if (ss.getSelectedId() != null) {
            renderHoverEntities.add(engine.getEntity(ss.getSelectedId()));
            if (selectedEntityIndex == -1 && hoveredEntities.size() > 0) {
                int index = (int) (System.currentTimeMillis() / 1000L) % hoveredEntities.size();
                renderHoverEntities.add(hoveredEntities.get(index));
            }
        } else {
            if (hoveredEntities.size() > 0) {
                int index = (int) (System.currentTimeMillis() / 1000L) % hoveredEntities.size();
                renderHoverEntities.add(hoveredEntities.get(index));
            }
        }
        for (Entity entity : renderHoverEntities) {
            if (entity == null) continue;
            TilePositionComponent tpc = entity.getComponent(TilePositionComponent.class);
            TilePoint tp = new TilePoint(tpc.tileX, tpc.tileY);
            Vector2 isoPosition = SpriteRender.calculateSpriteCenter(tp.toVector());
            isoPosition.scl(1, 1 / 4f);
            Table display = uiDisplayLabelCache.get(entity.getID());
            if (display != null) {
                float tileOffset = TILE_SIZE / 1.25f;
                display.setPosition(isoPosition.x * TILE_SIZE, (isoPosition.y * TILE_SIZE) + tileOffset, Align.bottom);
                display.act(delta);
                display.draw(batch, 1f);
            }
        }
    }

    private static class SpriteRender implements Comparable<Float> {

        Vector2 spriteCenter; // where the bottom of the sprite will be rendered, isometric/world coordinates
        float zOffset = 0; // how far up from the bottom of the sprite the z point is from the sprite, world coordinates
        String renderId;
        RenderType renderType;
        boolean centerY;
        float renderDelta = 0;
        Color color = Color.WHITE;

        public SpriteRender(String renderId, RenderType renderType, Vector2 spriteCenter, boolean centerY, float renderDelta) {
            this.renderId = renderId;
            this.renderType = renderType;
            this.spriteCenter = spriteCenter;
            this.centerY = centerY;
            this.renderDelta = renderDelta;
        }

        public float getZ() {
            return -(spriteCenter.y + zOffset);
        }

        @Override
        public int compareTo(Float o) {
            return (int) Math.signum(getZ() - o);
        }

        public static Vector2 calculateSpriteCenter(Vector2 standardTileCoords) {
            Vector3 iso3 = IsometricHelper.project(standardTileCoords);
            return new Vector2(iso3.x, iso3.y);
        }

        public void render(SpriteBatch batch) {
            TextureRegion texture = Sprites.getTexture(renderId, renderType, renderDelta);

            float displayX = spriteCenter.x * TILE_SIZE;
            float displayY = spriteCenter.y * TILE_SIZE;
            float width = texture.getRegionWidth() * 4f;
            float height = texture.getRegionHeight() * 4f;
            displayX -= width / 2f;
            if (centerY) {
                displayY -= height / 2f;
            }
            batch.setColor(color);
            batch.draw(texture, displayX, displayY, width, height);
            batch.setColor(Color.WHITE);
        }

        public void setColor(Color color) {
            this.color = color;
        }

    }

    /*private static class SpriteRender implements Comparable<Float> {

        private Sprite sprite;
        private Vector2 tileCenterPosition; // world coordinates, isometric

        SpriteRender()

        SpriteRender(Sprite sprite) {
            this.sprite = sprite;
        }

        SpriteRender(String spriteId) {
            this.sprite = Sprites.getSprite(spriteId);
        }

        void renderSprite(SpriteBatch batch, TilePoint tilePoint, boolean invert, boolean tile) {

            //Vector3 iso = IsometricHelper.project(new Vector2(tilePoint.getX(), tilePoint.getY()));
            float width = sprite.getWidth() * 4f;
            float height = sprite.getHeight() * 4f;
            Vector2 tileCenterPosition = getTileCenterPosition(tilePoint, sprite.getWidth(), sprite.getHeight());
            float displayX = tileCenterPosition.x;
            float displayY = tileCenterPosition.y;
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

        public static Vector2 getTileCenterPosition(TilePoint tilePoint, float width, float height) {
            Vector3 iso = IsometricHelper.project(new Vector2(tilePoint.getX(), tilePoint.getY()));
            width = width * 4f;
            height = height * 4f;
            float displayX = iso.x * TILE_SIZE;
            float displayY = (iso.y + 1) / 4 * TILE_SIZE;
            return new Vector2(displayX, displayY);
        }

        public float getZ() {
            return getTileCenterPosition(, TILE_SIZE, TILE_SIZE)
        }

        @Override
        public int compareTo(Float o) {
            return 
        }

    }*/

    private static Table createHoverTable(Entity entity, HoverInfoComponent hic, RenderSystem renderSystem) {
        /*
         * HOVER TEXT is a string that can request certain information from components.
         * All next lines will be ignored
         * If you want to display a variable from a component, use the following
         * 
         * $net.cmr.alchemycompany.component.ComponentClassName@variableName
         * 
         * If you want to round the amount of decimal places in something like a float,
         * append.
         * 
         * &round=2
         * 
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
        int indexOfAutoCommand = hoverText.indexOf("$autofill");
        if (indexOfAutoCommand != -1) {
            String autocompleted = HoverInfoComponent.getAutomaticHoverString(renderSystem.screen.getPlayerUUID(), entity);
            hoverText = hoverText.replace("$autofill", autocompleted);
        }
        Table table = new Table(Sprites.getSkin());
        table.debugAll();
        table.setBackground("window");
        table.pad(5);
        try (Scanner inputText = new Scanner(hoverText)) {
            StringBuilder inputBuilder = new StringBuilder();
            inputText.useDelimiter(" ");
            while (inputText.hasNext()) {
                String next = inputText.next();
                if (next.length() == 0) {
                    inputBuilder.append(" ");
                } else if (next.charAt(0) == '$') {
                    // Special event

                    /*if (!inputBuilder.isEmpty()) {
                        Label label = new Label(inputBuilder.toString(), skin);
                        inputBuilder = new StringBuilder();
                        table.add(label);
                    }*/

                    if (next.length() == 1) {
                        // Missing command
                        throw new IOException("Missing special action for HoverActionEntity.");
                    }

                    // If $\n is the next token, create a label with the string builder (if any is
                    // in there) and next row the table
                    if (next.length() > 1) {
                        if (next.charAt(1) == '\n') {
                            if (!inputBuilder.isEmpty()) {
                                Label label = new Label(inputBuilder.toString(), skin);
                                inputBuilder = new StringBuilder();
                                table.add(label);
                            }
                            table.row();
                            continue;
                        }
                    }

                    // split 0 will be action/component class
                    // split 1 will be the target/variable name
                    // split 2, 3... will be modifiers (rounding, truncating, etc...)
                    String[] split = next.substring(1).split("[@&]");
                    if (split.length < 2) {
                        throw new IOException(
                                "Missing essential information for special HoverEvent description with text: " + next);
                    }

                    if (split[0].equals("image")) {
                        // Create an image object
                        Sprite sprite = Sprites.getSprite(split[1]);
                        if (!inputBuilder.isEmpty()) {
                            Label label = new Label(inputBuilder.toString(), skin);
                            inputBuilder = new StringBuilder();
                            table.add(label);
                        }

                        float width = sprite.getWidth();
                        float height = sprite.getHeight();
                        float pad = 2;
                        for (int i = 2; i < split.length; i++) {
                            String modifierRaw = split[i];
                            String[] modifierTokens = modifierRaw.split("=");
                            if (modifierTokens.length != 2) {
                                throw new IOException("Improperly formatted modifier token: " + modifierRaw);
                            }
                            String modifierKey = modifierTokens[0];
                            String modifierValue = modifierTokens[1];

                            try {
                                switch (modifierKey) {
                                    case "width" -> width = Float.parseFloat(modifierValue);
                                    case "height" -> height = Float.parseFloat(modifierValue);
                                    case "pad" -> pad = Float.parseFloat(modifierValue);
                                    default -> throw new IOException("Modifier " + modifierKey + " not found");
                                }
                            } catch (Exception e) {
                                throw new IOException("Error parsing modifier " + modifierRaw, e);
                            }
                        }
                        
                        Image image = new Image(sprite);
                        table.add(image).size(width, height);
                    } else {
                        // Assume it is a component
                        JsonValue componentJson = componentStringMap.get(split[0]);
                        if (componentJson == null) {
                            // Doesn't have a component of this type, or it's formatted improperly. Either
                            // way, don't display it
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
                                throw new IOException("Improperly formatted modifier token: " + modifierRaw);
                            }
                            String modifierKey = modifierTokens[0];
                            String modifierValue = modifierTokens[1];

                            try {
                                switch (modifierKey) {
                                    case "round" -> roundAmount = Integer.parseInt(modifierValue);
                                    case "truncate" -> truncateAmount = Integer.parseInt(modifierValue);
                                    default -> throw new IOException("Modifier " + modifierKey + " not found");
                                }
                            } catch (Exception e) {
                                throw new IOException("Error parsing modifier " + modifierRaw, e);
                            }
                        }

                        String displayText = value.asString();

                        if (roundAmount != -1) {
                            Double doubleValue = null;
                            try {
                                doubleValue = Double.valueOf(displayText);
                            } catch (NumberFormatException nfe) {
                                throw new IOException("Cannot round value " + value.name + " if it is not a double");
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

                        /*Label label = new Label(displayText, skin);
                        table.add(label);*/
                        inputBuilder.append(displayText);
                    }
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
        table.pack();

        return table;
    }

    private boolean isInvert(int x, int y) {
        return (generateNoise(x, y) * 31) % 2 == 1;
    }

    private int generateNoise(int x, int y) {
        return (int) (x * 341873128712L + y * 132897987541L);
    }

    private int getNumberBetween(int x, int y, int min, int max) {
        return new Random(generateNoise(x, y)).nextInt((max - min) + 1) + min;
    }

    private String getSprite(Tile tile, int x, int y) {
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
                String[] options = new String[] { "SWAMP1", "SWAMP2", "SWAMP3", "SWAMP4", "SWAMP5" };
                int option = getNumberBetween(x, y, 0, 4);
                spriteType = options[option];
                break;
            default:
                break;
        }
        if (spriteType == null) {
            return null;
        }
        return spriteType;
    }

}
