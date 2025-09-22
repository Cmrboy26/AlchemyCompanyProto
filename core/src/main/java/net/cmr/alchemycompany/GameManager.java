package net.cmr.alchemycompany;

import java.util.UUID;

import com.badlogic.gdx.utils.Null;

import net.cmr.alchemycompany.component.BuildingComponent;
import net.cmr.alchemycompany.component.PurchaseCostComponent;
import net.cmr.alchemycompany.component.TilePositionComponent;
import net.cmr.alchemycompany.component.UnitComponent;
import net.cmr.alchemycompany.component.actions.AttackActionComponent;
import net.cmr.alchemycompany.component.actions.BuildingActionComponent;
import net.cmr.alchemycompany.component.actions.MovementActionComponent;
import net.cmr.alchemycompany.component.actions.PlayerActionComponent;
import net.cmr.alchemycompany.component.actions.SelectRecipeActionComponent;
import net.cmr.alchemycompany.ecs.Engine;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.entity.BuildingFactory;
import net.cmr.alchemycompany.entity.UnitFactory;
import net.cmr.alchemycompany.network.GameServer;
import net.cmr.alchemycompany.network.Stream;
import net.cmr.alchemycompany.network.packet.EntityPacket;
import net.cmr.alchemycompany.network.packet.EntityPacket.EntityState;
import net.cmr.alchemycompany.network.packet.TilePacket;
import net.cmr.alchemycompany.screen.GameScreen;
import net.cmr.alchemycompany.system.BuildingManagementSystem;
import net.cmr.alchemycompany.system.CombatSystem;
import net.cmr.alchemycompany.system.MovementSystem;
import net.cmr.alchemycompany.system.RecipeSystem;
import net.cmr.alchemycompany.system.RenderSystem;
import net.cmr.alchemycompany.system.ResearchSystem;
import net.cmr.alchemycompany.system.ResourceSystem;
import net.cmr.alchemycompany.system.SelectionSystem;
import net.cmr.alchemycompany.system.TurnSystem;
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

    public boolean tryPlaceUnit(UUID playerUUID, String type, int x, int y, boolean ignoreVisibility) {
        if (isClient()) {

        } else {
            Tile tile = engine.as(ACEngine.class).getWorld().getTile(x, y);
            if (tile != null && tile.canPlaceUnit()) {
                VisibilitySystem visibilitySystem = engine.getSystem(VisibilitySystem.class);
                if (ignoreVisibility || visibilitySystem == null || (visibilitySystem != null && visibilitySystem.isVisibleCurrently(playerUUID, x, y))) {
                    Entity unit = UnitFactory.createUnit(playerUUID, type, x, y);
                    UnitComponent uc = unit.getComponent(UnitComponent.class);
                    PurchaseCostComponent pcc = unit.getComponent(PurchaseCostComponent.class);
                    //if (bc.validPlacement.contains(tile.getFeature())) {
                        if (pcc != null) {
                            ResourceSystem resourceSystem = engine.getSystem(ResourceSystem.class);
                            if (resourceSystem != null) {
                                if (!resourceSystem.tryUseResources(playerUUID, pcc.getResourceCost(playerUUID, uc.unitId, engine))) {
                                    return false;
                                }
                            }
                        }

                        tile.setUnitSlotID(unit.getID()); // set tile occupied
                        engine.addEntity(unit);
                        return true;
                    //}
                }
            }
        }
        return false;
    }

    /*
     * When the player attempts to perform an action, send it to the server. The server will give a response if it is possible.
     */
    public boolean tryPlaceBuilding(UUID playerUUID, String type, int x, int y, boolean ignoreVisibility) {
        if (isClient()) {
            Entity buildAction = new Entity();
            buildAction.addComponent(new PlayerActionComponent(playerUUID), getEngine());
            buildAction.addComponent(new BuildingActionComponent(type, x, y), getEngine());
            clientStream.sendPacket(new EntityPacket(buildAction, EntityState.ADDED));
            return true;
        } else {
            boolean result = BuildingManagementSystem.tryPlaceBuilding(playerUUID, type, x, y, ignoreVisibility, engine);
            return result;
        }
    }

    public boolean tryRemoveBuilding(UUID playerUUID, int x, int y) {
        if (isClient()) {
            Entity buildAction = new Entity();
            buildAction.addComponent(new PlayerActionComponent(playerUUID), getEngine());
            buildAction.addComponent(new BuildingActionComponent(null, x, y), getEngine());
            clientStream.sendPacket(new EntityPacket(buildAction, EntityState.ADDED));
            return true;
        } else {
            boolean result = BuildingManagementSystem.tryRemoveBuilding(playerUUID, x, y, engine);
            return result;
        }
    }

    public boolean trySelectRecipe(UUID playerUUID, String recipeId, UUID buildingId) {
        if (isClient()) {
            Entity recipeAction = new Entity();
            recipeAction.addComponent(new PlayerActionComponent(playerUUID), getEngine());
            recipeAction.addComponent(new SelectRecipeActionComponent(recipeId, buildingId), getEngine());
            clientStream.sendPacket(new EntityPacket(recipeAction, EntityState.ADDED));
            return true;
        } else {
            boolean result = RecipeSystem.trySelectRecipe(playerUUID, engine.getEntity(buildingId), recipeId, engine);
            return result;
        }
    }

    public void tryMoveUnit(UUID playerUUID, UUID unitId, int x, int y) {
        if (isClient()) {
            Entity moveAction = new Entity();
            moveAction.addComponent(new PlayerActionComponent(playerUUID), engine);
            moveAction.addComponent(new MovementActionComponent(unitId, x, y), engine);
            clientStream.sendPacket(new EntityPacket(moveAction, EntityState.ADDED));
        }
    }

    public void tryAttackUnit(UUID playerUUID, UUID unitId, UUID targetId) {
        if (isClient()) {
            Entity attackAction = new Entity();
            attackAction.addComponent(new PlayerActionComponent(playerUUID), engine);
            attackAction.addComponent(new AttackActionComponent(unitId, targetId), engine);
            clientStream.sendPacket(new EntityPacket(attackAction, EntityState.ADDED));
        }
    }

    public static void onBuildingChange(UUID buildingplayerUUID, Entity buildingEntity, Engine engine) {
        TilePositionComponent tpc = buildingEntity.getComponent(TilePositionComponent.class);
        int x = tpc.tileX;
        int y = tpc.tileY;
        onPlacementChange(buildingplayerUUID, x, y, engine);
    }

    public static void onPlacementChange(UUID buildingplayerUUID, int x, int y, Engine engine) {
        engine.getSystem(VisibilitySystem.class).updateVisibility(buildingplayerUUID);
        engine.getSystem(ResourceSystem.class).calculateTurn(true);
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

    private static void addSharedSystems(ACEngine engine, World world) {
        engine.registerSystem(new VisibilitySystem());
        engine.registerSystem(new BuildingManagementSystem());
        engine.registerSystem(new RecipeSystem());
        engine.registerSystem(new ResearchSystem());
    }
    public static ACEngine createClientEngine(GameScreen screen, World world) {
        ACEngine engine = new ACEngine();
        engine.setWorld(world);
        engine.registerSystem(new RenderSystem(screen));
        engine.registerSystem(new SelectionSystem());
        engine.registerSystem(new ResourceSystem(screen));
        engine.registerSystem(new MovementSystem(true));

        addSharedSystems(engine, world);
        return engine;
    }
    public static ACEngine createServerEngine(GameServer server, World world) {
        ACEngine engine = new ACEngine();
        engine.setWorld(world);
        engine.registerSystem(new ResourceSystem());
        engine.registerSystem(new TurnSystem(server));
        engine.registerSystem(new CombatSystem());
        engine.registerSystem(new MovementSystem(false));

        addSharedSystems(engine, world);
        return engine;
    }

}
