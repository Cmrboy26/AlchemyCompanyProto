package net.cmr.alchemycompany.component;

import java.util.UUID;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class OwnerComponent extends Component {

    public String playerID;

    public OwnerComponent() {}

    public OwnerComponent(UUID playerID) {
        this.playerID = playerID.toString();
    }

    public UUID getUUID() {
        return UUID.fromString(playerID);
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "playerID");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        this.playerID = json.readValue("playerID", String.class, jsonData);
    }

}
