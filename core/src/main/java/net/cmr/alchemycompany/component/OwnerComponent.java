package net.cmr.alchemycompany.component;

import java.util.UUID;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class OwnerComponent extends Component {

    private String playerUUID;

    public OwnerComponent() { super(); }

    public OwnerComponent(UUID playerUUID) {
        super();
        this.playerUUID = playerUUID.toString();
    }

    public UUID getUUID() {
        return UUID.fromString(playerUUID);
    }

    @Override
    public void write(Json json) {
        json.writeValue("playerUUID", playerUUID, String.class);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        this.playerUUID = json.readValue("playerUUID", String.class, jsonData);
    }

}
