package net.cmr.alchemycompany;

import java.util.UUID;

import net.cmr.alchemycompany.component.BuildingComponent;
import net.cmr.alchemycompany.component.FogOfWarComponent;
import net.cmr.alchemycompany.component.OwnerComponent;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.entity.BuildingFactory;
import net.cmr.alchemycompany.entity.BuildingFactory.BuildingType;
import net.cmr.alchemycompany.system.BuildingManagementSystem;
import net.cmr.alchemycompany.system.RenderSystem;
import net.cmr.alchemycompany.system.ResourceSystem;
import net.cmr.alchemycompany.system.VisibilitySystem;
import net.cmr.alchemycompany.world.Tile;
import net.cmr.alchemycompany.world.World;

/**
 * Stores engine and world objects and provides helper methods for tasks like placing buildings, queuing research, and moving units for clients.
 */
public class GameManager {

    private ACEngine engine;
    private final boolean isClient;

    public GameManager(final boolean isClient, ACEngine engine, World world) {
        this.isClient = isClient;
        this.engine = engine;
        this.engine.setWorld(world);
    }

    @BroadcastUpdate
    public void updateTile(int x, int y) {
        if (isClient) return;
    }

    /*
     * When the player attempts to perform an action, send it to the server. The server will give a response if it is possible.
     */

    public boolean tryPlaceBuilding(UUID playerID, BuildingType type, int x, int y, boolean ignoreVisibility) {
        Tile tile = getWorld().getTile(x, y);
        if (tile == null || !tile.canPlaceBuilding()) return false;
        VisibilitySystem visibilitySystem = engine.getSystem(VisibilitySystem.class);
        if (!ignoreVisibility && visibilitySystem != null && !visibilitySystem.isVisibleCurrently(playerID, x, y)) return false;
        Entity building = BuildingFactory.createBuilding(playerID, type, x, y);
        BuildingComponent bc = building.getComponent(BuildingComponent.class);
        if (!bc.validPlacement.contains(tile.getFeature())) return false;
        tile.setBuildingSlotID(building.getID()); // set tile occupied
        updateTile(x, y); // update clients
        engine.addEntity(building);
        onBuildingChange(playerID, x, y);
        return true;
    }

    public boolean tryRemoveBuilding(UUID playerID, int x, int y) {
        Tile tile = getWorld().getTile(x, y);
        if (tile == null || tile.isBuildingSlotEmpty()) return false;
        Entity building = engine.getEntity(tile.getBuildingSlotID());
        BuildingComponent bc = building.getComponent(BuildingComponent.class);
        if (bc.buildingType == BuildingType.HEADQUARTERS) return false;
        System.out.println(bc.buildingType);
        UUID buildingOwner = building.getComponent(OwnerComponent.class).getUUID();
        if (!playerID.equals(buildingOwner)) return false;
        tile.setBuildingSlotID(null); // set tile unoccupied
        updateTile(x, y);
        engine.removeEntity(building);
        onBuildingChange(buildingOwner, x, y);
        return true;
    }

    public void onBuildingChange(UUID buildingPlayerID, int x, int y) {
        engine.getSystem(VisibilitySystem.class).updateVisibility(buildingPlayerID);
        engine.getSystem(ResourceSystem.class).calculateTurn();
    }

    public boolean isClient() {
        return isClient;
    }
    public ACEngine getEngine() {
        return engine;
    }
    public World getWorld() {
        return engine.getWorld();
    }

    public static @interface BroadcastUpdate {

    }

    private static void addSharedSystems(ACEngine engine, World world) {
        engine.registerSystem(new ResourceSystem());
        engine.registerSystem(new VisibilitySystem());
        engine.registerSystem(new BuildingManagementSystem());
    }
    public static ACEngine createClientEngine(World world) {
        ACEngine engine = new ACEngine();
        engine.registerSystem(new RenderSystem(world));

        addSharedSystems(engine, world);
        return engine;
    }
    public static ACEngine createServerEngine(World world) {
        ACEngine engine = new ACEngine();

        Entity fogOfWarEntity = new Entity();
        fogOfWarEntity.addComponent(new FogOfWarComponent(), engine);
        engine.addEntity(fogOfWarEntity);

        addSharedSystems(engine, world);
        return engine;
    }

}
