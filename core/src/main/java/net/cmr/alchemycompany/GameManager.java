package net.cmr.alchemycompany;

import java.util.UUID;

import com.badlogic.gdx.utils.Null;

import net.cmr.alchemycompany.component.BuildingComponent;
import net.cmr.alchemycompany.component.FogOfWarComponent;
import net.cmr.alchemycompany.component.actions.BuildingActionComponent;
import net.cmr.alchemycompany.component.actions.PlayerActionComponent;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.entity.BuildingFactory;
import net.cmr.alchemycompany.entity.BuildingFactory.BuildingType;
import net.cmr.alchemycompany.network.Stream;
import net.cmr.alchemycompany.network.packet.EntityPacket;
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
    private final Stream clientStream;
    public static final String CLIENT_ONLY_MESSAGE = "Method should only be called on the client side.";

    public GameManager(@Null final Stream clientStream, ACEngine engine, World world) {
        this.clientStream = clientStream;
        this.engine = engine;
        this.engine.setWorld(world);
    }

    @BroadcastUpdate
    public void updateTile(int x, int y) {
        if (isClient()) return;
    }

    /*
     * When the player attempts to perform an action, send it to the server. The server will give a response if it is possible.
     */

    public boolean tryPlaceBuilding(UUID playerID, BuildingType type, int x, int y, boolean ignoreVisibility) {
        if (isClient()) {
            Entity buildAction = new Entity();
            buildAction.addComponent(new PlayerActionComponent(playerID), getEngine());
            buildAction.addComponent(new BuildingActionComponent(type, x, y), getEngine());
            clientStream.sendPacket(new EntityPacket(buildAction, true));
            return true;
        } else {
            boolean result = BuildingManagementSystem.tryPlaceBuilding(playerID, type, x, y, ignoreVisibility, engine);
            return result;
        }
    }

    public boolean tryRemoveBuilding(UUID playerID, int x, int y) {
        if (isClient()) {
            Entity buildAction = new Entity();
            buildAction.addComponent(new PlayerActionComponent(playerID), getEngine());
            buildAction.addComponent(new BuildingActionComponent(null, x, y), getEngine());
            clientStream.sendPacket(new EntityPacket(buildAction, true));
            return true;
        } else {
            boolean result = BuildingManagementSystem.tryRemoveBuilding(playerID, x, y, engine);
            return result;
        }
    }

    public boolean isClient() {
        return clientStream != null;
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
