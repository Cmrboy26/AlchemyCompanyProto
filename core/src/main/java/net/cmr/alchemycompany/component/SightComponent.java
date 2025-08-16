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
        json.writeField(this, "radius");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        json.readField(this, "radius", jsonData);
    }

}
