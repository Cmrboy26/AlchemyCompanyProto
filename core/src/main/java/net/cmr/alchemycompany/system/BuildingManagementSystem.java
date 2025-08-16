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

            System.out.println("WE RECIEVED AN ENTITY!!!");
            System.out.println(pac);
            System.out.println(bac);


            Tile tile = engine.as(ACEngine.class).getWorld().getTile(x, y);
            if (tile == null || !tile.canPlaceBuilding()) {
                engine.removeEntity(entity);
                continue;
            }
            VisibilitySystem visibilitySystem = engine.getSystem(VisibilitySystem.class);
            if (visibilitySystem != null && !visibilitySystem.isVisibleCurrently(playerID, x, y)) {
                engine.removeEntity(entity);
                continue;
            }
            Entity building = BuildingFactory.createBuilding(playerID, type, x, y);
            BuildingComponent bc = building.getComponent(BuildingComponent.class);
            if (!bc.validPlacement.contains(tile.getFeature())) {
                engine.removeEntity(entity);
                continue;
            }
            tile.setBuildingSlotID(building.getID()); // set tile occupied
            //updateTile(x, y); // update clients
            engine.addEntity(building);
            onBuildingChange(playerID, x, y);

            engine.removeEntity(entity);
        }
    }

    public void onBuildingChange(UUID buildingPlayerID, int x, int y) {
        engine.getSystem(VisibilitySystem.class).updateVisibility(buildingPlayerID);
        engine.getSystem(ResourceSystem.class).calculateTurn();
    }

}
