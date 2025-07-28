package net.cmr.alchemycompany;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import net.cmr.alchemycompany.BuildingAction.ConstructAction;
import net.cmr.alchemycompany.Resources.Resource;
import net.cmr.alchemycompany.Sprites.SpriteType;
import net.cmr.alchemycompany.World.WorldFeature;
import net.cmr.alchemycompany.troops.HealthHolder;
import net.cmr.alchemycompany.troops.Scout;
import net.cmr.alchemycompany.troops.Soldier;
import net.cmr.alchemycompany.troops.Troop;
import net.cmr.alchemycompany.troops.Troop.AttackType;
import net.cmr.alchemycompany.troops.Troop.EquipmentHolder;
import net.cmr.alchemycompany.troops.Troop.FightInformation;
import net.cmr.alchemycompany.troops.Troop.TroopConstructorFunction;

public abstract class Building implements HealthHolder {

    protected World world;
    private SpriteType type;
    protected Player player;
    private int x, y;
    private float health;
    protected boolean idle = false; // purely a display state
    protected boolean destroyed = false; // functional state
    protected boolean built = false;
    protected Set<BuildingAction> buildingActions;

    public Building(SpriteType type, BuildingContext context) {
        this.type = type;
        this.world = context.world;
        this.player = context.player;
        this.x = context.x;
        this.y = context.y;
        this.buildingActions = new HashSet<>();
        this.built = false;
        this.health = getMaxHealth();
    }

    public SpriteType getSpriteType() {
        return type;
    }

    public void renderTile(SpriteBatch batch, float tileSize) {
        if (idle) {
            batch.setColor(Color.GRAY);
        }
        if (!built) {
            batch.setColor(Color.YELLOW); // Not built yet, display in yellow
        }
        if (destroyed) {
            batch.setColor(Color.RED);
        }
        batch.draw(Sprites.getTexture(type), x * tileSize, y * tileSize, tileSize, tileSize);
        batch.setColor(Color.WHITE);
    }

    public Table onClick(Skin skin) {
        Table table = new Table(skin);

        table.add("Owner: " + player.toString()).fillX().pad(10).row();
        table.add("Name: " + getClass().getSimpleName().replaceAll("Building", "")).fillX().pad(10).row();

        Label healthLabel = new Label("Health: "+health+"/"+getMaxHealth(), skin) {
            @Override
            public void act(float delta) {
                setText("Health: "+health+"/"+getMaxHealth());
                super.act(delta);
            }
        };
        table.add(healthLabel).growX().pad(10).row();
        Label statusLabel = new Label("Constructing... (idktbh turn(s) left)", skin) {
            @Override
            public void act(float delta) {
                if (destroyed) {
                    setText("Destroyed");
                } else if (built) {
                    setText(idle ? "Idle" : "Active");
                } else {
                    setText("Constructing... ("+getConstructionTime()+" turn(s) left)");
                }
                super.act(delta);
            }
        };
        table.add(statusLabel).growX().pad(10).row();

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

    public void addBuildingAction(BuildingAction action) {
        action.setBuilding(this);
        buildingActions.add(action);
    }

    public void stepActions() {
        LinkedList<BuildingAction> toRemove = new LinkedList<>();
        for (BuildingAction action : buildingActions) {
            boolean remove = action.step();
            if (remove) {
                toRemove.add(action);
            }
        }
        while (!toRemove.isEmpty()) {
            buildingActions.remove(toRemove.getLast());
            toRemove.removeLast();
        }
    }

    public void onTurn() {

    }

    @SuppressWarnings("unchecked")
    public <T extends BuildingAction> T findAction(Class<T> clazz) {
        for (BuildingAction action : buildingActions) {
            if (clazz.isInstance(action)) {
                return (T) action;
            }
        }
        return null;
    }

    public int getConstructionTime() {
        return findAction(ConstructAction.class).getTurnsRemaining();
    }

    public boolean canFunction() {
        return !destroyed && built;
    }
    public void setIdle() { idle = true; }
    public void setActive() { idle = false; }

    @Override
    public boolean processAttack(FightInformation attack, HealthHolder attacker) {
        health -= attack.getStrength();
        if (attacker instanceof EquipmentHolder) {
            EquipmentHolder eqh = (EquipmentHolder) attacker;
            eqh.useEquipment();
        }
        if (health <= 0) {
            destroyed = true;
            health = 0;
            player.updateResourceDisplay();

            if (this instanceof HeadquarterBuilding) {
                // BUILDING DESTROYED, GAME OVER
                System.out.println("GAME OVER! HQ DESTROYED.");
            }
            return true;
        }
        return false;
    }

    @Override
    public float getHealth() {
        return health;
    }

    @Override
    public FightInformation getDefense(FightInformation attack, HealthHolder attacker) {
        return modifyFightWithEquipment(new FightInformation(0, AttackType.NORMAL), attacker, false);
    }
    @Override
    public FightInformation getAttack(HealthHolder defender) {
        return FightInformation.NO_ATTACK;
    }

    @Override
    public float getMaxHealth() {
        return 50;
    }

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

    @Override
    public String toString() {
        return getClass().getSimpleName().replaceAll("Building", "")+"#"+((hashCode()+"").substring(6));
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
            this.x = Math.max(Math.min(world.width - 1, x), 0);
            this.y = Math.max(Math.min(world.height - 1, y), 0);
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

    public static class HeadquarterBuilding extends AbstractStorageBuilding implements ProductionBuilding {
        public HeadquarterBuilding(BuildingContext context) { super(SpriteType.HEADQUARTERS, context); }
        @Override public WorldFeature[] getAllowedTiles() { return new WorldFeature[]{ WorldFeature.PLAINS, WorldFeature.SWAMP, WorldFeature.FOREST, WorldFeature.CRYSTAL_VALLEY }; }
        @Override public Resource[] getAllowedStoreResources() {
            return Resource.values();
        }
        @Override public float getMaxAmount() { return 10; }
        @Override public Map<Resource, Float> getProductionPerTurn() { return Resources.start().add(Resource.GOLD, 1f).add(Resource.SCIENCE, 5f).build(); }
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
        Resource specifiedResource = Resource.WATER; // Default resource for storage
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
                    player.updateResourceDisplay();
                }
            });

            return table;
        }
        @Override public float getMaxAmount() {
            return 25;
        }
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
                case GOLD:
                    cpt.put(Resource.IRON, 1f);
                    cpt.put(Resource.SULFURIC_ACID, 1f);
                    cpt.put(Resource.CRYSTAL, 1f);
                    break;
                case TRANSMUTATE_COPPER_TO_IRON:
                    cpt.put(Resource.SULFUR, 1f);
                    cpt.put(Resource.COPPER, 1f);
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
                case GOLD:
                    rpt.put(Resource.GOLD, 1f);
                    break;
                case TRANSMUTATE_COPPER_TO_IRON:
                    rpt.put(Resource.IRON, 1f);
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
            GOLD,
            TRANSMUTATE_COPPER_TO_IRON,

        }

        @Override
        public Table onClick(Skin skin) {
            Table table = super.onClick(skin);
            SelectBox<FactoryRecipes> selectionBox = new SelectBox<>(skin);
            selectionBox.setItems(FactoryRecipes.values());
            selectionBox.setSelected(selectedRecipe);
            table.add(selectionBox).growX().row();

            // Create a table with two columns: left for input, right for output
            Table resourcesTable = new Table(skin);

            // Add headers
            resourcesTable.add(new Label("Input Resources:", skin)).pad(5);
            resourcesTable.add(new Label("Output Resources:", skin)).pad(5).row();

            // Get input and output resources
            Map<Resource, Float> consumption = getConsumptionPerTurn();
            Map<Resource, Float> production = getProductionPerTurn();

            // Find max rows needed
            int maxRows = Math.max(consumption.size(), production.size());
            Resource[] inputKeys = consumption.keySet().toArray(new Resource[0]);
            Resource[] outputKeys = production.keySet().toArray(new Resource[0]);

            for (int i = 0; i < maxRows; i++) {
                // Input column
                if (i < inputKeys.length) {
                    Resource resource = inputKeys[i];
                    Float amount = consumption.get(resource);
                    resourcesTable.add(new Label(resource.toString() + ": " + amount, skin)).pad(5);
                } else {
                    resourcesTable.add().pad(5);
                }
                // Output column
                if (i < outputKeys.length) {
                    Resource resource = outputKeys[i];
                    Float amount = production.get(resource);
                    resourcesTable.add(new Label(resource.toString() + ": " + amount, skin)).pad(5);
                } else {
                    resourcesTable.add().pad(5);
                }
                resourcesTable.row();
            }

            table.add(resourcesTable).growX().row();

            // Add listener to update resource table when recipe changes
            selectionBox.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    selectedRecipe = selectionBox.getSelected();
                    player.updateResourceDisplay();

                    // Clear and repopulate resources table
                    resourcesTable.clear();

                    resourcesTable.add(new Label("Input Resources:", skin)).pad(5);
                    resourcesTable.add(new Label("Output Resources:", skin)).pad(5).row();

                    Map<Resource, Float> newConsumption = getConsumptionPerTurn();
                    Map<Resource, Float> newProduction = getProductionPerTurn();

                    int maxRows = Math.max(newConsumption.size(), newProduction.size());
                    Resource[] inputKeys = newConsumption.keySet().toArray(new Resource[0]);
                    Resource[] outputKeys = newProduction.keySet().toArray(new Resource[0]);

                    for (int i = 0; i < maxRows; i++) {
                        if (i < inputKeys.length) {
                            Resource resource = inputKeys[i];
                            Float amount = newConsumption.get(resource);
                            resourcesTable.add(new Label(resource.toString() + ": " + amount, skin)).pad(5);
                        } else {
                            resourcesTable.add().pad(5);
                        }
                        if (i < outputKeys.length) {
                            Resource resource = outputKeys[i];
                            Float amount = newProduction.get(resource);
                            resourcesTable.add(new Label(resource.toString() + ": " + amount, skin)).pad(5);
                        } else {
                            resourcesTable.add().pad(5);
                        }
                        resourcesTable.row();
                    }

                    table.invalidate();
                    table.pack();
                    if (table.getParent() instanceof Window) {
                        ((Window) table.getParent()).invalidate();
                        ((Window) table.getParent()).pack();
                    }
                }
            });

            return table;
        }
    }

    public static class StatueBuilding extends Building implements ProductionBuilding {
        public StatueBuilding(BuildingContext context) { super(SpriteType.STATUE, context); }
        @Override public Map<Resource, Float> getProductionPerTurn() { return Resources.singleItem(Resource.SCIENCE, 3f); }
        
    }

    public static class ResearchLabBuilding extends Building implements ConsumptionBuilding, ProductionBuilding {
        public ResearchLabBuilding(BuildingContext context) { super(SpriteType.RESEARCH_LAB, context); }
        @Override public WorldFeature[] getAllowedTiles() { return new WorldFeature[]{ WorldFeature.PLAINS, WorldFeature.SWAMP, WorldFeature.FOREST }; }
        @Override public Map<Resource, Float> getProductionPerTurn() { return Resources.singleItem(Resource.SCIENCE, 15f); }
        @Override public Map<Resource, Float> getConsumptionPerTurn() { return Resources.singleItem(Resource.SULFURIC_ACID, 2f); }
    }

    public static class ArcherTowerBuilding extends Building implements ConsumptionBuilding {
        public ArcherTowerBuilding(BuildingContext context) { super(SpriteType.ARCHER_TOWER, context); }
        @Override public WorldFeature[] getAllowedTiles() { return new WorldFeature[]{ WorldFeature.PLAINS, WorldFeature.SWAMP, WorldFeature.FOREST, WorldFeature.MOUNTAINS, WorldFeature.CRYSTAL_VALLEY }; }
        @Override public Map<Resource, Float> getConsumptionPerTurn() { return Resources.singleItem(Resource.IRON, 1f); }

        @Override
        public void onTurn() {
            if (!idle) {
                Iterator<Vector2> iterator = World.getSpiralIterator(getX(), getY(), World.getSpiralRadius(2));
                while (iterator.hasNext()) {
                    Vector2 next = iterator.next();
                    int tileX = (int) next.x;
                    int tileY = (int) next.y;
                    Troop troopAround = world.getTroopAt(tileX, tileY);
                    if (troopAround != null) {
                        // ATTACK IT!!!!
                        // Normally, check if the troop is from a different player
                        System.out.println("Attack "+troopAround);
                        FightInformation attack = getAttack(troopAround);
                        troopAround.processAttack(attack, this);
                    }
                }
            }
        }

        @Override
        public FightInformation getAttack(HealthHolder defender) {
            return new FightInformation(15, AttackType.NORMAL);
        }
    }

    public static class BarracksBuilding extends Building implements ConsumptionBuilding {
        BarracksRecipe selectedRecipe = BarracksRecipe.NONE;
        public int troopConstructionTime = 0;

        public BarracksBuilding(BuildingContext context) { super(SpriteType.BARRACKS, context); }
        @Override public WorldFeature[] getAllowedTiles() { return new WorldFeature[]{ WorldFeature.PLAINS, WorldFeature.SWAMP, WorldFeature.FOREST }; }
        @Override public Map<Resource, Float> getConsumptionPerTurn() {
            Map<Resource, Float> cpt = new HashMap<>();
            switch (selectedRecipe) {
                case SCOUT:
                    cpt.put(Resource.GOLD, 3f);
                    break;
                case SOLDIER:
                    cpt.put(Resource.IRON, 2f);
                    cpt.put(Resource.GOLD, 3f);
                    break;
                case NONE:
                default:
                    break;
            }
            return cpt;
        }
        
        public int getTroopConstructTime() {
            return troopConstructionTime;
        }

        public void queueTroop(BarracksRecipe recipe) {
            this.selectedRecipe = recipe;
            troopConstructionTime = recipe.time;
        }

        public enum BarracksRecipe {
            NONE,
            SCOUT(2, Scout::new),
            SOLDIER(4, Soldier::new)
            ;

            public final int time;
            public final TroopConstructorFunction tcf;
            BarracksRecipe(int time, TroopConstructorFunction tcf) {
                this.time = time;
                this.tcf = tcf;
            }
            BarracksRecipe() {
                this.time = 0;
                this.tcf = null;
            }
        }

        @Override
        public void onTurn() {
            if (selectedRecipe == BarracksRecipe.NONE) {
                return;
            }
            if (canFunction() && !idle) {
                troopConstructionTime--;
            }
            if (getTroopConstructTime() <= 0) {
                Iterator<Vector2> iterator = World.getSpiralIterator(getX(), getY());
                while (iterator.hasNext()) {
                    Vector2 next = iterator.next();
                    Troop troopAt = world.getTroopAt((int) next.x, (int) next.y);
                    if (troopAt == null) {
                        Troop troop = selectedRecipe.tcf.apply(new BuildingContext(world, player, (int) next.x, (int) next.y));
                        world.placeTroop(troop);
                        break;
                    }
                }

                queueTroop(BarracksRecipe.NONE);
            }
        }
    }

}
