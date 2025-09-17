package net.cmr.alchemycompany.component;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.ecs.Engine;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.ecs.Family;

public class PurchaseCostComponent extends Component {

    private HashMap<String, Float> resourceCost, resourceCostScale;

    public PurchaseCostComponent() { }
    public PurchaseCostComponent(HashMap<String, Float> resourceCost, HashMap<String, Float> resourceCostScale) {
        this.resourceCost = resourceCost;
        this.resourceCostScale = resourceCostScale;
    }

    @Override
    public void write(Json json) {
        json.writeObjectStart("resourceCost");
        for (Entry<String, Float> entry : resourceCost.entrySet()) {
            json.writeValue(entry.getKey(), entry.getValue(), Float.class);
        }
        json.writeObjectEnd();
        json.writeObjectStart("resourceCostScale");
        for (Entry<String, Float> entry : resourceCostScale.entrySet()) {
            json.writeValue(entry.getKey(), entry.getValue());
        }
        json.writeObjectEnd();
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        this.resourceCost = new HashMap<>();
        JsonValue resourceCost = jsonData.get("resourceCost");
        if (resourceCost != null) {
            for (JsonValue entry = resourceCost.child; entry != null; entry = entry.next) {
                String key = entry.name;
                Float value = entry.asFloat();
                this.resourceCost.put(key, value);
            }
        }

        this.resourceCostScale = new HashMap<>();
        JsonValue resourceCostScale = jsonData.get("resourceCostScale");
        if (resourceCostScale != null) {
            for (JsonValue entry = resourceCostScale.child; entry != null; entry = entry.next) {
                String key = entry.name;
                Float value = entry.asFloat();
                this.resourceCostScale.put(key, value);
            }
        }
    }

    public int getExistingCount(UUID playerUUID, String buildingId, Engine engine) {
        int existingBuildings = 0;
        for (Entity entity : engine.getEntities(Family.all(BuildingComponent.class, OwnerComponent.class))) {
            OwnerComponent owner = entity.getComponent(OwnerComponent.class);
            BuildingComponent building = entity.getComponent(BuildingComponent.class);
            if (owner != null && owner.getUUID() != null && owner.getUUID().equals(playerUUID)
                && building != null && building.buildingId != null && building.buildingId.equals(buildingId)) {
                existingBuildings++;
            }
        }
        return existingBuildings;
    }

    public HashMap<String, Float> getResourceCost(UUID playerUUID, String buildingId, Engine engine) {
        int existingBuildings = getExistingCount(playerUUID, buildingId, engine);
        return getResourceCost(existingBuildings);
    }

    public HashMap<String, Float> getResourceCost(int existingBuildings) {
        HashMap<String, Float> updatedResourceCost = new HashMap<>(resourceCost);
        for (Entry<String, Float> entry : resourceCostScale.entrySet()) {
            String resource = entry.getKey();
            Float scale = entry.getValue();
            updatedResourceCost.put(resource, updatedResourceCost.getOrDefault(resource, 0f) + scale * existingBuildings);
        }
        return updatedResourceCost;
    }

}
