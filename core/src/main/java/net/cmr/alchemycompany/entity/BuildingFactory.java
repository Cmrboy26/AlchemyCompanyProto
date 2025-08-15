package net.cmr.alchemycompany.entity;

import java.beans.Visibility;
import java.util.UUID;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter.OutputType;

import net.cmr.alchemycompany.component.BuildingComponent;
import net.cmr.alchemycompany.component.ConstructionComponent;
import net.cmr.alchemycompany.component.HealthComponent;
import net.cmr.alchemycompany.component.OwnerComponent;
import net.cmr.alchemycompany.component.SightComponent;
import net.cmr.alchemycompany.component.TilePositionComponent;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.system.VisibilitySystem;

public class BuildingFactory {
    
    public enum BuildingType {
        HEADQUARTERS,
        FARM,
    }

    public static Entity createBuilding(UUID playerID, BuildingType type, int x, int y) {
        Entity building = createEmptyBuilding(type);
        building.addComponent(new TilePositionComponent(x, y), null);
        building.addComponent(new OwnerComponent(playerID), null);
        building.addComponent(new HealthComponent(100), null);
        building.addComponent(new ConstructionComponent(5), null);
        building.addComponent(new SightComponent(VisibilitySystem.DEFAULT_BUILDING_RADIUS), null);
        System.out.println(building);
        return building;
    }

    public static Entity createEmptyBuilding(BuildingType type) {
        Json json = new Json();
        JsonReader reader = new JsonReader();
        JsonValue values = reader.parse(Gdx.files.internal("assets/gamedata/buildings.json"));

        JsonValue building = values.get(type.name());
        System.out.println(building.toJson(OutputType.json));
        Entity entity = json.fromJson(Entity.class, building.toJson(OutputType.json));

        return entity;

        /*boolean read = true;

        if (read) {
            Entity building = new Json().fromJson(Entity.class, 
            "{\"id\":\"4dcee4bf-b235-4710-8684-4ad738cbac0e\",\"components\":[{\"type\":\"net.cmr.alchemycompany.component.ResearchRequirementComponent\",\"component\":{\"technologiesRequired\":[\"AGRICULTURE\"]}},{\"type\":\"net.cmr.alchemycompany.component.ProducerComponent\",\"component\":{\"production\":{\"IRON\":1}}},{\"type\":\"net.cmr.alchemycompany.component.BuildingComponent\",\"component\":{\"buildingType\":\"FARM\",\"validPlacement\":[\"PLAINS\"]}},{\"type\":\"net.cmr.alchemycompany.component.RenderComponent\",\"component\":{\"spriteType\":\"FARM\"}}]}"
            );

            return building;
        } else {

            Entity building = new Entity();
            SpriteType sprite = null;
            HashSet<WorldFeature> placeableFeatures = null;
            switch (type) {
                case HEADQUARTERS:
                    sprite = SpriteType.HEADQUARTERS;
                    placeableFeatures = BuildingComponent.exclude(WorldFeature.WATER);

                    building.addComponent(new StorageComponent(new HashMap<>(), Resources.allItems(10f)), null);
                    HashMap<AttackType, Float> defenseMap = new HashMap<>();
                    defenseMap.put(AttackType.NORMAL, 2f);
                    building.addComponent(new DefenseComponent(50, defenseMap), null);
                    break;
                case FARM:
                    sprite = SpriteType.FARM;
                    placeableFeatures = BuildingComponent.only(WorldFeature.PLAINS);
                    building.addComponent(new ProducerComponent(Resources.singleItem(Resource.IRON, 1f)), null);
                    building.addComponent(new ResearchRequirementComponent(Technology.AGRICULTURE), null);
                    break;
                default:
                    break;
            }
            building.addComponent(new RenderComponent(sprite), null);
            building.addComponent(new BuildingComponent(type, placeableFeatures), null);

            Json json = new Json(OutputType.json);
            System.out.println(json.toJson(building).toString());
            System.out.println(json.prettyPrint(building));
            
            return building;
        }*/
    }

}
