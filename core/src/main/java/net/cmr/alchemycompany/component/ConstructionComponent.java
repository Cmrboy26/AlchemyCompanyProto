package net.cmr.alchemycompany.component;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class ConstructionComponent extends Component {

    public int turns;

    public ConstructionComponent() {}

    public ConstructionComponent(int turns) {
        this.turns = turns;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "turns");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        this.turns = json.readValue("turns", Integer.class, jsonData);
    }
    
}
