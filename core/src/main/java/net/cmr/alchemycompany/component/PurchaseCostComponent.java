package net.cmr.alchemycompany.component;

import java.util.HashMap;
import java.util.Map.Entry;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class PurchaseCostComponent extends Component {

    public HashMap<String, Float> resourceCost, resourceCostScale;

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
                this.resourceCost.put(key, value);
            }
        }
    }

}
