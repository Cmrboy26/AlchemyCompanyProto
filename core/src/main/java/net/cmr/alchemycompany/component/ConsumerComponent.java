package net.cmr.alchemycompany.component;

import java.util.HashMap;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.game.Resources.Resource;

public class ConsumerComponent extends Component {

    public HashMap<Resource, Float> consumption;

    public ConsumerComponent() {}
    
    public ConsumerComponent(HashMap<Resource, Float> consumption) {
        this.consumption = consumption;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "consumption");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        this.consumption = Component.readMap(Resource.class, Float.class, jsonData.get("consumption"), json);
    }

}
