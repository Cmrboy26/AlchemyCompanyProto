package net.cmr.alchemycompany;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import net.cmr.alchemycompany.Resources.Resource;
import net.cmr.alchemycompany.Sprites.SpriteType;
import net.cmr.alchemycompany.World.WorldFeature;

public abstract class Building {

    protected World world;
    private SpriteType type;
    protected Player player;
    private int x, y;
    protected boolean idle = false; // purely a display state
    protected boolean destroyed = false; // functional state

    public Building(SpriteType type, BuildingContext context) {
        this.type = type;
        this.world = context.world;
        this.player = context.player;
        this.x = context.x;
        this.y = context.y;
    }

    public SpriteType getSpriteType() {
        return type;
    }

    public void renderTile(SpriteBatch batch, float tileSize) {
        batch.draw(Sprites.getTexture(type), x * tileSize, y * tileSize, tileSize, tileSize);
    }

    public Table onClick(Skin skin) {
        Table table = new Table(skin);

        table.add("Owner: " + player.toString()).fillX().pad(10).row();
        table.add("Name: " + getClass().getSimpleName().replaceAll("Building", "")).fillX().pad(10).row();

        Label idleLabel = new Label("Idle: "+idle, skin) {
            @Override
            public void act(float delta) {
                setText("Idle: "+idle);
                super.act(delta);
            }
        };
        table.add(idleLabel).fillX().pad(10).row();

        return table;
    }

    public WorldFeature[] getAllowedTiles() {
        return WorldFeature.values();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean canFunction() {
        //return !idle && !destroyed;
        return !destroyed;
    }
    public void setIdle() { idle = true; }
    public void setActive() { idle = false; }

    @Override
    public int hashCode() {
        return Objects.hash(player, x, y, getClass().getSimpleName());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Building) {
            Building building = (Building) obj;
            if (building.x == x && building.y == y) {
                if (getPlayer().equals(building.getPlayer())) {
                    return true;
                }
            }
        }
        return false;
    }

    @FunctionalInterface
    public static interface BuildingConstructorFunction {
        Building apply(BuildingContext context);
    }

    public static class BuildingContext {
        public Player player;
        public int x, y;
        public World world;
        public BuildingContext(World world, Player player, int x, int y) {
            this.world = world;
            this.player = player;
            this.x = x;
            this.y = y;
        }
    }

    public interface ProductionBuilding {
        Map<Resource, Float> getProductionPerTurn();
    }
    public interface ConsumptionBuilding {
        Map<Resource, Float> getConsumptionPerTurn();
    }
    public static abstract class AbstractStorageBuilding extends Building {

        Map<Resource, Float> resourceMap;

        public AbstractStorageBuilding(SpriteType type, BuildingContext context) {
            super(type, context);
            resourceMap = new HashMap<>();
        }

        public abstract float getMaxAmount();
        public float getAmountStored(Resource resource) { return resourceMap.getOrDefault(resource, 0f); }
        protected void setAmountStored(Resource resource, float amount) {
            resourceMap.put(resource, amount);
            if (amount > getMaxAmount()) {
                throw new RuntimeException("Stored more than maximum amount in storage building: (" + amount + " > " + getMaxAmount() + ")");
            }
        }
        protected Map<Resource, Float> getStorageMap() {
            return resourceMap;
        }

        /**
         * @return amount of resource that cannot be added
         */
        public float addAmount(Resource resource, float amount) {
            if (amount < 0) { throw new RuntimeException("Cannot add negative amount in addAmount method"); }
            if (getAmountStored(resource) + amount > getMaxAmount()) {
                float amountToReturn = getAmountStored(resource) + amount - getMaxAmount();
                setAmountStored(resource, getMaxAmount());
                return amountToReturn;
            }
            setAmountStored(resource, getAmountStored(resource) + amount);
            return 0;
        }

        public void consumeAmount(Resource resource, float amount) {
            if (amount < 0) { throw new RuntimeException("Cannot consume negative amount in consumeAmount method"); }
            setAmountStored(resource, getAmountStored(resource) - amount);
        }

        public abstract Resource[] getAllowedStoreResources();

    }

    // Buildings

    public static class HeadquarterBuilding extends AbstractStorageBuilding {
        public HeadquarterBuilding(BuildingContext context) { super(SpriteType.HEADQUARTERS, context); }
        @Override public WorldFeature[] getAllowedTiles() { return new WorldFeature[]{ WorldFeature.PLAINS, WorldFeature.SWAMP, WorldFeature.FOREST, WorldFeature.CRYSTAL_VALLEY }; }
        @Override public Resource[] getAllowedStoreResources() {
            return Resource.values();
        }
        @Override
        public float getMaxAmount() { return 5; }
    }

    public static class ExtractorBuilding extends Building implements ProductionBuilding {
        public ExtractorBuilding(BuildingContext context) { super(SpriteType.EXTRACTOR, context); }
        @Override public WorldFeature[] getAllowedTiles() { return new WorldFeature[]{ WorldFeature.WATER, WorldFeature.SWAMP, WorldFeature.MOUNTAINS, WorldFeature.CRYSTAL_VALLEY }; }

        @Override public Map<Resource, Float> getProductionPerTurn() {
            WorldFeature featureBelow = world.getFeature(getX(), getY());
            Map<Resource, Float> map = new HashMap<>();
            switch (featureBelow) {
                case WATER:
                    map.put(Resource.WATER, 5f);
                    break;
                case SWAMP:
                    map.put(Resource.WITCH_EYE, 1f);
                    map.put(Resource.SULFUR, 1f);
                    break;
                case MOUNTAINS:
                    map.put(Resource.IRON, 1f);
                    map.put(Resource.COPPER, 1f);
                    break;
                case CRYSTAL_VALLEY:
                    map.put(Resource.CRYSTAL, 1f);
                    break;
                default:
                    break;
            }
            return map;
        }
    }

    public static class StorageBuilding extends AbstractStorageBuilding {
        Resource specifiedResource;
        public StorageBuilding(BuildingContext context) { super(SpriteType.STORAGE, context); }
        @Override public WorldFeature[] getAllowedTiles() { return new WorldFeature[]{ WorldFeature.PLAINS, WorldFeature.SWAMP }; }
        @Override public Table onClick(Skin skin) {
            Table table = super.onClick(skin);
            SelectBox<Resource> selectionBox = new SelectBox<>(skin);
            selectionBox.setItems(Resource.values());
            selectionBox.setSelected(specifiedResource);
            table.add(selectionBox).growX().row();
            selectionBox.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (specifiedResource != selectionBox.getSelected()) {
                        resourceMap.clear();
                    }
                    specifiedResource = selectionBox.getSelected();
                    player.updateResourceConsumption();
                }
            });

            return table;
        }
        @Override public float getMaxAmount() { return 25; }
        @Override public Resource[] getAllowedStoreResources() {
            if (specifiedResource == null) {
                return null;
            }
            return new Resource[] {specifiedResource};
        }
    }

    public static class FactoryBuilding extends Building implements ConsumptionBuilding, ProductionBuilding {
        FactoryRecipes selectedRecipe = FactoryRecipes.NONE;

        public FactoryBuilding(BuildingContext context) { super(SpriteType.FACTORY, context); }
        @Override public WorldFeature[] getAllowedTiles() { return new WorldFeature[]{ WorldFeature.PLAINS, WorldFeature.SWAMP }; }
        @Override public Map<Resource, Float> getConsumptionPerTurn() {
            Map<Resource, Float> cpt = new HashMap<>();
            switch (selectedRecipe) {
                case SULFURIC_ACID:
                    cpt.put(Resource.SULFUR, 1f);
                    cpt.put(Resource.WATER, 1f);
                    break;
                case BATTERY:
                    cpt.put(Resource.IRON, 1f);
                    cpt.put(Resource.SULFURIC_ACID, 1f);
                    break;
                case NONE:
                default:
                    break;
            }
            return cpt;
        }
        @Override public Map<Resource, Float> getProductionPerTurn() {
            Map<Resource, Float> rpt = new HashMap<>();
            switch (selectedRecipe) {
                case SULFURIC_ACID:
                    rpt.put(Resource.SULFURIC_ACID, 1f);
                    break;
                case BATTERY:
                    rpt.put(Resource.GOLD, 1f);
                    break;
                case NONE:
                default:
                    break;
            }
            return rpt;
        }

        private enum FactoryRecipes {
            NONE,
            SULFURIC_ACID,
            BATTERY,
        }

        @Override
        public Table onClick(Skin skin) {
            Table table = super.onClick(skin);
            SelectBox<FactoryRecipes> selectionBox = new SelectBox<>(skin);
            selectionBox.setItems(FactoryRecipes.values());
            selectionBox.setSelected(selectedRecipe);
            table.add(selectionBox).growX().row();
            selectionBox.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    selectedRecipe = selectionBox.getSelected();
                    player.updateResourceConsumption();
                }
            });

            return table;
        }
    }

    public static class StatueBuilding extends Building {
        public StatueBuilding(BuildingContext context) { super(SpriteType.STATUE, context); }
    }

    public static class ResearchLabBuilding extends Building {
        public ResearchLabBuilding(BuildingContext context) { super(SpriteType.RESEARCH_LAB, context); }
        @Override public WorldFeature[] getAllowedTiles() { return new WorldFeature[]{ WorldFeature.PLAINS, WorldFeature.SWAMP, WorldFeature.FOREST }; }
    }

    public static class ArcherTowerBuilding extends Building {
        public ArcherTowerBuilding(BuildingContext context) { super(SpriteType.ARCHER_TOWER, context); }
        @Override public WorldFeature[] getAllowedTiles() { return new WorldFeature[]{ WorldFeature.PLAINS, WorldFeature.SWAMP, WorldFeature.FOREST, WorldFeature.MOUNTAINS, WorldFeature.CRYSTAL_VALLEY }; }
    }

    public static class BarracksBuilding extends Building {
        public BarracksBuilding(BuildingContext context) { super(SpriteType.BARRACKS, context); }
        @Override public WorldFeature[] getAllowedTiles() { return new WorldFeature[]{ WorldFeature.PLAINS, WorldFeature.SWAMP, WorldFeature.FOREST }; }
    }

}
