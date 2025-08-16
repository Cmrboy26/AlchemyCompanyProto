package net.cmr.alchemycompany.component.actions;

import java.util.UUID;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.component.Component;

public class PlayerActionComponent extends Component implements IActionComponent {

    public UUID playerUUID;

    public PlayerActionComponent() { }
    public PlayerActionComponent(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    @Override
    public void write(Json json) {
        json.writeValue("playerUUID", playerUUID != null ? playerUUID.toString() : null);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        String uuidStr = jsonData.getString("playerUUID", null);
        playerUUID = uuidStr != null ? UUID.fromString(uuidStr) : null;
    }

}
