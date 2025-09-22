package net.cmr.alchemycompany.component;

import java.util.HashMap;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class ProducerComponent extends Component {

    public HashMap<String, Float> production;

    public ProducerComponent() {}
    
    public ProducerComponent(HashMap<String, Float> production) {
        this.production = production;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "production");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        json.readField(this, "production", jsonData);
    }

}
