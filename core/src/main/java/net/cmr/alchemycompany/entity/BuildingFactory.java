package net.cmr.alchemycompany.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter.OutputType;

import net.cmr.alchemycompany.component.BuildingComponent;
import net.cmr.alchemycompany.component.HealthComponent;
import net.cmr.alchemycompany.component.OwnerComponent;
import net.cmr.alchemycompany.component.SightComponent;
import net.cmr.alchemycompany.component.TilePositionComponent;
import net.cmr.alchemycompany.component.UnitComponent;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.system.VisibilitySystem;

public class BuildingFactory {

    private static EntityReader<String> reader;

    public static Entity createEmptyBuilding(String unitId) {
        if (reader == null) {
            reader = new EntityReader<>();
            reader.readEntities(Gdx.files.internal("gamedata/buildings.json"), (e) -> { return e.getComponent(BuildingComponent.class).buildingId; });
        }
        return reader.getEntity(unitId);
    }    

    public static Entity createBuilding(UUID playerID, String buildingId, int x, int y) {
        Entity building = createEmptyBuilding(buildingId);
        building.addComponent(new TilePositionComponent(x, y), null);
        building.addComponent(new OwnerComponent(playerID), null);
        building.addComponent(new HealthComponent(100), null);
        if (!building.hasComponent(SightComponent.class)) {
            building.addComponent(new SightComponent(VisibilitySystem.DEFAULT_BUILDING_RADIUS), null);
        }
        return building;
    }

    public static Map<String, Entity> getRegisteredBuildingEntities() {
        return reader.cloneEntityMap();
    }

}
