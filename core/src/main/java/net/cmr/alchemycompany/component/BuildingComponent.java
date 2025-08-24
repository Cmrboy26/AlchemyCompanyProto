package net.cmr.alchemycompany.component;

import java.util.HashSet;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.world.World.WorldFeature;

public class BuildingComponent extends Component {

    public String buildingId;
    public HashSet<WorldFeature> validPlacement;

    public BuildingComponent() {}

    public BuildingComponent(String buildingId, HashSet<WorldFeature> validPlacement) {
        this.buildingId = buildingId;
        this.validPlacement = validPlacement;
    }

    public static HashSet<WorldFeature> only(WorldFeature...features) {
        HashSet<WorldFeature> set = new HashSet<>();
        for (WorldFeature feature : features) {
            set.add(feature);
        }
        return set;
    }

    public static HashSet<WorldFeature> exclude(WorldFeature...features) {
        HashSet<WorldFeature> set = new HashSet<>();
        for (WorldFeature feature : WorldFeature.values()) {
            boolean excluded = false;
            for (WorldFeature checkFeature : features) {
                if (feature == checkFeature) {
                    excluded = true;
                    break;
                }
            }
            if (!excluded) {
                set.add(feature);
            }
        }
        return set;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "buildingId");
        json.writeField(this, "validPlacement");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        this.buildingId = json.readValue("buildingId", String.class, jsonData);
        HashSet<WorldFeature> validFeatures = json.readValue(HashSet.class, WorldFeature.class, null, jsonData.get("validPlacement"));
        HashSet<WorldFeature> invalidFeatures = json.readValue(HashSet.class, WorldFeature.class, null, jsonData.get("invalidPlacement"));
        if (validFeatures != null) {
            this.validPlacement = only(validFeatures.toArray(new WorldFeature[0]));
        } else if (invalidFeatures != null) {
            this.validPlacement = exclude(invalidFeatures.toArray(new WorldFeature[0]));
        } else {
            throw new RuntimeException("No valid or invalid world feature list provided.");
        }
    }

}
