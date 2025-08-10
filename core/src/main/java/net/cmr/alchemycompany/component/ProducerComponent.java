package net.cmr.alchemycompany.component;

import java.util.HashMap;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.game.Resources.Resource;

public class ProducerComponent extends Component {

    public HashMap<Resource, Float> production;

    public ProducerComponent() {}
    
    public ProducerComponent(HashMap<Resource, Float> production) {
        this.production = production;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "production");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        this.production = Component.readMap(Resource.class, Float.class, jsonData.get("production"), json);
    }

}
