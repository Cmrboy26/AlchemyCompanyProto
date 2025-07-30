package net.cmr.alchemycompany;

import java.util.UUID;

import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.entity.BuildingFactory;
import net.cmr.alchemycompany.entity.BuildingFactory.BuildingType;
import net.cmr.alchemycompany.world.Tile;
import net.cmr.alchemycompany.world.World;

public class GameManager {

    private World world;
    private ACEngine engine;
    private final boolean isClient;

    public GameManager(final boolean isClient, ACEngine engine, World world) {
        this.isClient = isClient;
        this.engine = engine;
        this.world = world;
    }

    @BroadcastUpdate
    public void updateTile(int x, int y) {
        if (isClient) return;
    }    

    public boolean tryPlaceBuilding(UUID playerID, BuildingType type, int x, int y) {
        Tile tile = world.getTile(x, y);
        if (tile == null || !tile.canPlaceBuilding()) return false;
        Entity building = BuildingFactory.createBuilding(type, x, y);
        tile.setBuildingSlotID(building.getID()); // set tile occupied
        updateTile(x, y); // update clients
        engine.addEntity(building); // add entity
        return true;
    }

    public boolean isClient() {
        return isClient;
    }
    public ACEngine getEngine() {
        return engine;
    }
    public World getWorld() {
        return world;
    }

    public static @interface BroadcastUpdate {
        
    }

}
