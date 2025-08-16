package net.cmr.alchemycompany.component.actions;

import java.util.UUID;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.component.Component;
import net.cmr.alchemycompany.entity.BuildingFactory.BuildingType;

public class BuildingActionComponent extends Component implements IActionComponent {

    public BuildingType type = null; // If null, request remove building
    public int x, y;

    public BuildingActionComponent() { }
    public BuildingActionComponent(BuildingType type, int x, int y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }

    @Override
    public void write(Json json) {
        json.writeValue("type", type == null ? null : type.name());
        json.writeValue("x", x);
        json.writeValue("y", y);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        String typeName = jsonData.getString("type", null);
        type = typeName == null ? null : BuildingType.valueOf(typeName);
        x = jsonData.getInt("x", 0);
        y = jsonData.getInt("y", 0);
    }



}
