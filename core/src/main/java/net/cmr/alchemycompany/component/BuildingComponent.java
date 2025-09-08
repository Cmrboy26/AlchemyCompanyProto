package net.cmr.alchemycompany.component;

import java.util.HashSet;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.world.World.WorldFeature;

public class BuildingComponent extends Component {

    public String buildingId;

    public BuildingComponent() {}

    public BuildingComponent(String buildingId, HashSet<WorldFeature> validPlacement) {
        this.buildingId = buildingId;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "buildingId");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        this.buildingId = json.readValue("buildingId", String.class, jsonData);
    }

}
