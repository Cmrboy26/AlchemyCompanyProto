package net.cmr.alchemycompany.system;

import java.util.Set;

import net.cmr.alchemycompany.component.BuildingComponent;
import net.cmr.alchemycompany.component.TilePositionComponent;
import net.cmr.alchemycompany.ecs.Engine;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.ecs.EntitySystem;

public class TileManagementSystem extends EntitySystem {

    private Set<Entity> tilePositionEntities;
    private Set<Entity> buildingEntities;

    @Override
    public void addedToEngine(Engine engine) {
        tilePositionEntities = engine.getComponentMapper(TilePositionComponent.class);
        buildingEntities = engine.getComponentMapper(BuildingComponent.class);
    }

    @Override
    public void removedFromEngine(Engine engine) {

    }
    
    

}
