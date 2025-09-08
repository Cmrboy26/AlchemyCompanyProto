package net.cmr.alchemycompany.system;

import java.util.UUID;

import net.cmr.alchemycompany.ACEngine;
import net.cmr.alchemycompany.GameManager;
import net.cmr.alchemycompany.ITurnSystem;
import net.cmr.alchemycompany.IUpdateSystem;
import net.cmr.alchemycompany.component.ConstructionComponent;
import net.cmr.alchemycompany.component.OwnerComponent;
import net.cmr.alchemycompany.component.PlacementComponent;
import net.cmr.alchemycompany.component.PurchaseCostComponent;
import net.cmr.alchemycompany.component.UnitComponent;
import net.cmr.alchemycompany.component.actions.IActionComponent;
import net.cmr.alchemycompany.component.actions.PlayerActionComponent;
import net.cmr.alchemycompany.component.actions.UnitActionComponent;
import net.cmr.alchemycompany.ecs.Engine;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.ecs.EntitySystem;
import net.cmr.alchemycompany.ecs.Family;
import net.cmr.alchemycompany.entity.UnitFactory;
import net.cmr.alchemycompany.world.Tile;

public class UnitManagementSystem extends EntitySystem implements IUpdateSystem, ITurnSystem {

    Family unitActionFamily;

    @Override
    public void addedToEngine(Engine engine) {
        unitActionFamily = Family.all(PlayerActionComponent.class, UnitActionComponent.class);
    }

    @Override
    public void update(float delta) {
        IActionComponent.processActionEntities(engine, UnitActionComponent.class, (entity) -> {
            PlayerActionComponent pac = entity.getComponent(PlayerActionComponent.class);
            UnitActionComponent uac = entity.getComponent(UnitActionComponent.class);

            UUID playerID = pac.playerUUID;
            int x = uac.x;
            int y = uac.y;
            String type = uac.type;

            System.out.println("UnitManagementSystem recieved action "+pac+"\n"+uac);

            if (type != null) {
                if (tryPlaceUnit(playerID, type, x, y, false, engine.as(ACEngine.class))) {
                    GameManager.onBuildingChange(playerID, entity, engine);
                }
            } else {
                if (tryRemoveUnit(playerID, x, y, engine.as(ACEngine.class))) {
                    GameManager.onPlacementChange(playerID, x, y, engine);
                }
            }
        });
    }

    public static boolean tryRemoveUnit(UUID playerID, int x, int y, ACEngine engine) {
        Tile tile = engine.as(ACEngine.class).getWorld().getTile(x, y);
        if (tile != null && !tile.canPlaceUnit()) {
            // Remove tile at location if it is the players
            Entity unit = engine.getEntity(tile.getUnitSlotID());
            UnitComponent uc = unit.getComponent(UnitComponent.class);
            UUID unitOwner = unit.getComponent(OwnerComponent.class).getUUID();
            if (playerID.equals(unitOwner)) {
                tile.setUnitSlotID(null); // set tile unoccupied
                engine.removeEntity(unit);
                return true;
            }
        }
        return false;
    }

    public static boolean tryPlaceUnit(UUID playerID, String type, int x, int y, boolean overrideVisibility, ACEngine engine) {
        Tile tile = engine.as(ACEngine.class).getWorld().getTile(x, y);
        if (tile != null && tile.canPlaceUnit()) {
            VisibilitySystem visibilitySystem = engine.getSystem(VisibilitySystem.class);
            if (overrideVisibility || visibilitySystem == null || (visibilitySystem != null && visibilitySystem.isVisibleCurrently(playerID, x, y))) {
                Entity unit = UnitFactory.createUnit(playerID, type, x, y);
                UnitComponent uc = unit.getComponent(UnitComponent.class);
                PurchaseCostComponent pcc = unit.getComponent(PurchaseCostComponent.class);
                PlacementComponent pc = unit.getComponent(PlacementComponent.class);
                if (pc.getValidPlacement().contains(tile.getFeature())) {
                    if (pcc != null) {
                        ResourceSystem resourceSystem = engine.getSystem(ResourceSystem.class);
                        if (resourceSystem != null) {
                            if (!resourceSystem.tryUseResources(playerID, pcc.getResourceCost(playerID, uc.unitId, engine))) {
                                return false;
                            }
                        }
                    }

                    tile.setUnitSlotID(unit.getID()); // set tile occupied
                    engine.addEntity(unit);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onTurn() {
        Family constructingUnits = Family.all(UnitComponent.class, ConstructionComponent.class);
        for (Entity entity : engine.getEntities(constructingUnits)) {
            ConstructionComponent cc = entity.getComponent(ConstructionComponent.class);
            cc.turns--;
            if (cc.turns <= 0) {
                entity.removeComponent(ConstructionComponent.class, engine);
            }
            engine.changedEntity(entity);
        }
    }

    @Override
    public int getTurnPriority() {
        return 2;
    }

}
