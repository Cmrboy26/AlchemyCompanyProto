package net.cmr.alchemycompany.component;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class SightComponent extends Component {

    public int radius = 0;

    public SightComponent() { }
    public SightComponent(int radius) {
        this.radius = radius;
    }

    @Override
    public void write(Json json) {
        json.writeValue(radius);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        radius = json.readValue(Integer.class, jsonData);
    }
    
}
