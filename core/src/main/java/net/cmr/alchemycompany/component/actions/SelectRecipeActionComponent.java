package net.cmr.alchemycompany.component.actions;

import java.util.UUID;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.component.Component;

public class SelectRecipeActionComponent extends Component implements IActionComponent {

    public String recipeId = null; // If null, remove recipe
    public String buildingId = null;

    public SelectRecipeActionComponent() { }
    public SelectRecipeActionComponent(String recipeId, UUID buildingId) {
        this.recipeId = recipeId;
        this.buildingId = buildingId.toString();
    }

    public UUID getBuildingId() {
        return UUID.fromString(buildingId);
    }

    @Override
    public void write(Json json) {
        json.writeValue("recipeId", recipeId);
        json.writeValue("buildingId", buildingId);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        recipeId = jsonData.getString("recipeId");
        buildingId = jsonData.getString("buildingId");
    }

}
