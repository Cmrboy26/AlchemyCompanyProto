package net.cmr.alchemycompany.entity;

import net.cmr.alchemycompany.component.BuildingComponent;
import net.cmr.alchemycompany.component.TilePositionComponent;
import net.cmr.alchemycompany.ecs.Entity;

public class BuildingFactory {
    
    public enum BuildingType {
        FARM
    }

    public static Entity createBuilding(BuildingType type, int x, int y) {
        Entity building = new Entity();
        building.addComponent(new TilePositionComponent(x, y), null);
        building.addComponent(new BuildingComponent(), null);
        switch (type) {
            case FARM:
                break;
            default:
                break;
        }
        return building;
    }

}
