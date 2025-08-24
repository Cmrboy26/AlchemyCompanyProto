package net.cmr.alchemycompany.component;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class LabelComponent extends Component {
    
    public String name, description;

    public LabelComponent() {}

    public LabelComponent(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "name");
        json.writeField(this, "description");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        this.name = json.readValue("name", String.class, jsonData);
        this.description = json.readValue("description", String.class, jsonData);
    }
    
}
