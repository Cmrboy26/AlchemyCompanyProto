package net.cmr.alchemycompany.entity;

import java.util.UUID;

import com.badlogic.gdx.Gdx;

import net.cmr.alchemycompany.component.BuildingComponent;
import net.cmr.alchemycompany.component.HealthComponent;
import net.cmr.alchemycompany.component.OwnerComponent;
import net.cmr.alchemycompany.component.SightComponent;
import net.cmr.alchemycompany.component.TilePositionComponent;
import net.cmr.alchemycompany.component.UnitComponent;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.system.VisibilitySystem;

public class UnitFactory {
    
    private static EntityReader<String> reader;
    
    public static EntityReader<String> getReader() {
        if (reader == null) {
            reader = new EntityReader<>();
            reader.readEntities(Gdx.files.internal("gamedata/units.json"), (e) -> { return e.getComponent(UnitComponent.class).unitId; });
        }
        return reader;
    }

    public static Entity createEmptyUnit(String unitId) {
        return getReader().getEntity(unitId);
    }    

    public static Entity createUnit(UUID playerID, String unitId, int x, int y) {
        Entity building = createEmptyUnit(unitId);
        building.addComponent(new TilePositionComponent(x, y), null);
        building.addComponent(new OwnerComponent(playerID), null);
        if (!building.hasComponent(SightComponent.class)) {
            building.addComponent(new SightComponent(VisibilitySystem.DEFAULT_UNIT_RADIUS), null);
        }
        return building;
    }

}