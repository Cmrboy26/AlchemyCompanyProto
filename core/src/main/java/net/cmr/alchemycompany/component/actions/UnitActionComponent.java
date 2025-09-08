package net.cmr.alchemycompany.component.actions;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.component.Component;

public class UnitActionComponent extends Component implements IActionComponent {

    public String type = null; // If null, request remove
    public int x, y;

    public UnitActionComponent() { }
    public UnitActionComponent(String type, int x, int y) {
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
