package net.cmr.alchemycompany.component;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class ScienceComponent extends Component {
    
    public float sciencePerTurn;

    public ScienceComponent() {}

    public ScienceComponent(float sciencePerTurn) {
        this.sciencePerTurn = sciencePerTurn;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "sciencePerTurn");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        this.sciencePerTurn = json.readValue("sciencePerTurn", Float.class, jsonData);
    }

}
