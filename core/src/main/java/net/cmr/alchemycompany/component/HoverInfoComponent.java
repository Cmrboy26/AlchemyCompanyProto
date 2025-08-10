package net.cmr.alchemycompany.component;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class HoverInfoComponent extends Component {
    
    public String hoverText;

    public HoverInfoComponent() {}

    public HoverInfoComponent(String hoverText) {
        this.hoverText = hoverText;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "hoverText");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        this.hoverText = json.readValue("hoverText", String.class, jsonData);
    }

}
