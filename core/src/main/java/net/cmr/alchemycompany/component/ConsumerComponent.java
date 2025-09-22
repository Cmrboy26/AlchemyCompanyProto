package net.cmr.alchemycompany.component;

import java.util.HashMap;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class ConsumerComponent extends Component {

    public HashMap<String, Float> consumption;

    public ConsumerComponent() {}
    
    public ConsumerComponent(HashMap<String, Float> consumption) {
        this.consumption = consumption;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "consumption");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        json.readField(this, "consumption", jsonData);
        //this.consumption = Component.readMap(String.class, Float.class, jsonData.get("consumption"), json);
    }

}
