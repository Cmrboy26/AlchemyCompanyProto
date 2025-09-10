package net.cmr.alchemycompany.component.actions;

import java.util.List;
import java.util.UUID;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.Null;

import net.cmr.alchemycompany.component.Component;
import net.cmr.alchemycompany.world.TilePoint;

public class AttackActionComponent extends Component implements IActionComponent {

    private UUID entityUUID, targetUUID;
    public AttackActionComponent() { }
    public AttackActionComponent(UUID entityUUID, UUID targetUUID) {
        this.entityUUID = entityUUID;
        this.targetUUID = targetUUID;
    }

    @Override
    public void write(Json json) {
        json.writeValue("entityUUID", entityUUID != null ? entityUUID.toString() : null);
        json.writeValue("targetUUID", targetUUID != null ? targetUUID.toString() : null);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        String entityStr = jsonData.getString("entityUUID", null);
        entityUUID = entityStr != null ? UUID.fromString(entityStr) : null;
        String targetStr = jsonData.getString("targetUUID", null);
        targetUUID = targetStr != null ? UUID.fromString(targetStr) : null;
    }

    public UUID getEntityUUID() { return entityUUID; }
    public UUID getTargetUUID() { return targetUUID; }
    
}
