package net.cmr.alchemycompany.component.actions;

import java.util.UUID;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.component.Component;

public class BuildingActionComponent extends Component implements IActionComponent {

    public String type = null; // If null, request remove building
    public int x, y;

    public BuildingActionComponent() { }
    public BuildingActionComponent(String type, int x, int y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }

    @Override
    public void write(Json json) {
        json.writeValue("type", type == null ? null : type);
        json.writeValue("x", x);
        json.writeValue("y", y);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        type = jsonData.getString("type", null);
        x = jsonData.getInt("x", 0);
        y = jsonData.getInt("y", 0);
    }



}
