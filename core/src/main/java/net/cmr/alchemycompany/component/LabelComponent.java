package net.cmr.alchemycompany.component;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class LabelComponent extends Component {
    
    public String name;

    public LabelComponent() {}

    public LabelComponent(String name) {
        this.name = name;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "name");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        this.name = json.readValue("name", String.class, jsonData);
    }
    
}
