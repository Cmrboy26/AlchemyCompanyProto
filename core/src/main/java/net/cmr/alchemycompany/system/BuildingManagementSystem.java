package net.cmr.alchemycompany.system;

import java.util.Set;
import java.util.UUID;

import net.cmr.alchemycompany.ACEngine;
import net.cmr.alchemycompany.IUpdateSystem;
import net.cmr.alchemycompany.component.BuildingComponent;
import net.cmr.alchemycompany.component.OwnerComponent;
import net.cmr.alchemycompany.component.actions.BuildingActionComponent;
import net.cmr.alchemycompany.component.actions.PlayerActionComponent;
import net.cmr.alchemycompany.ecs.Engine;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.ecs.EntitySystem;
import net.cmr.alchemycompany.ecs.Family;
import net.cmr.alchemycompany.entity.BuildingFactory;
import net.cmr.alchemycompany.entity.BuildingFactory.BuildingType;
import net.cmr.alchemycompany.world.Tile;

public class BuildingManagementSystem extends EntitySystem implements IUpdateSystem {

    Family buildingActionFamily;

    @Override
    public void addedToEngine(Engine engine) {
        buildingActionFamily = Family.all(PlayerActionComponent.class, BuildingActionComponent.class);
    }

    @Override
    public void update(float delta) {
        Set<Entity> entities = engine.getEntities(buildingActionFamily);
        for (Entity entity : entities) {
            PlayerActionComponent pac = entity.getComponent(PlayerActionComponent.class);
            BuildingActionComponent bac = entity.getComponent(BuildingActionComponent.class);

            UUID playerID = pac.playerUUID;
            int x = bac.x;
            int y = bac.y;
            BuildingType type = bac.type;

            System.out.println("BuildManagementSystem recieved action "+pac+"\n"+bac);

            if (type != null) {
                if (tryPlaceBuilding(playerID, type, x, y, false, engine.as(ACEngine.class))) {
                    onBuildingChange(playerID, x, y, engine);
                }
            } else {
                if (tryRemoveBuilding(playerID, x, y, engine.as(ACEngine.class))) {
                    onBuildingChange(playerID, x, y, engine);
                }
            }

            engine.removeEntity(entity);
        }
    }

    public static void onBuildingChange(UUID buildingPlayerID, int x, int y, Engine engine) {
        engine.getSystem(VisibilitySystem.class).updateVisibility(buildingPlayerID);
        engine.getSystem(ResourceSystem.class).calculateTurn();
    }

    public static boolean tryRemoveBuilding(UUID playerID, int x, int y, ACEngine engine) {
        Tile tile = engine.as(ACEngine.class).getWorld().getTile(x, y);
        if (tile != null && !tile.canPlaceBuilding()) {
            // Remove tile at location if it is the players
            Entity building = engine.getEntity(tile.getBuildingSlotID());
            BuildingComponent bc = building.getComponent(BuildingComponent.class);
            if (bc.buildingType != BuildingType.HEADQUARTERS) {
                UUID buildingOwner = building.getComponent(OwnerComponent.class).getUUID();
                if (playerID.equals(buildingOwner)) {
                    tile.setBuildingSlotID(null); // set tile unoccupied
                    engine.removeEntity(building);
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean tryPlaceBuilding(UUID playerID, BuildingType type, int x, int y, boolean overrideVisibility, ACEngine engine) {
        Tile tile = engine.as(ACEngine.class).getWorld().getTile(x, y);
        if (tile != null && tile.canPlaceBuilding()) {
            VisibilitySystem visibilitySystem = engine.getSystem(VisibilitySystem.class);
            if (overrideVisibility || visibilitySystem == null || (visibilitySystem != null && visibilitySystem.isVisibleCurrently(playerID, x, y))) {
                Entity building = BuildingFactory.createBuilding(playerID, type, x, y);
                BuildingComponent bc = building.getComponent(BuildingComponent.class);
                if (bc.validPlacement.contains(tile.getFeature())) {
                    tile.setBuildingSlotID(building.getID()); // set tile occupied
                    engine.addEntity(building);
                    return true;
                }
            }
        }
        return false;
    }

}
