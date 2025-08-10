package net.cmr.alchemycompany.component;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class MovementComponent extends Component {
    
    public float movesPerTurn, movesRemaining;

    public MovementComponent() {}

    public MovementComponent(float movesPerTurn, float movesRemaining) {
        this.movesPerTurn = movesPerTurn;
        this.movesRemaining = movesRemaining;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "movesPerTurn");
        json.writeField(this, "movesRemaining");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        this.movesPerTurn = json.readValue("movesPerTurn", Float.class, jsonData);
        this.movesRemaining = json.readValue("movesRemaining", Float.class, jsonData);
    }

}
