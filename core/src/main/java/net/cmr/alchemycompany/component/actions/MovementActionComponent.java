package net.cmr.alchemycompany.component.actions;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.Null;

import net.cmr.alchemycompany.component.Component;
import net.cmr.alchemycompany.world.TilePoint;

public class MovementActionComponent extends Component implements IActionComponent {

    private UUID entityUUID;
    private int targetX;
    private int targetY;
    private @Null List<TilePoint> authorizedMovementPath;

    public MovementActionComponent() { }
    public MovementActionComponent(UUID entityUUID, int targetX, int targetY) {
        this.entityUUID = entityUUID;
        this.targetX = targetX;
        this.targetY = targetY;
    }
    public MovementActionComponent(UUID entityUUID, int targetX, int targetY, List<TilePoint> authorizedMovementPath) {
        this(entityUUID, targetX, targetY);
        this.authorizedMovementPath = authorizedMovementPath;
    }

    @Override
    public void write(Json json) {
        json.writeValue("entityUUID", entityUUID != null ? entityUUID.toString() : null);
        json.writeValue("targetX", targetX);
        json.writeValue("targetY", targetY);
        /*if (authorizedMovementPath != null) {
            json.writeArrayStart("authorizedMovementPath");
            for (TilePoint tp : authorizedMovementPath) {
                json.writeArrayStart();
                json.writeValue(tp.getX());
                json.writeValue(tp.getY());
                json.writeArrayEnd();
            }
            json.writeArrayEnd();
        }*/
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        String uuidStr = jsonData.getString("entityUUID", null);
        entityUUID = uuidStr != null ? UUID.fromString(uuidStr) : null;
        this.targetX = json.readValue("targetX", Integer.class, jsonData);
        this.targetY = json.readValue("targetY", Integer.class, jsonData);
        /*if (jsonData.hasChild("authorizedMovementPath")) {
            authorizedMovementPath = new LinkedList<>();
            for (JsonValue entry = jsonData.child; entry != null; entry = entry.child) {
                int x = entry.get(0).asInt();
                int y = entry.get(1).asInt();
                TilePoint tp = new TilePoint(x, y);
                authorizedMovementPath.add(tp);
            }
        }*/
    }

    public UUID getEntityUUID() { return entityUUID; }
    public int getTargetX() { return targetX; }
    public int getTargetY() { return targetY; }
    //public List<TilePoint> getAuthorizedMovementPath() { return authorizedMovementPath; }
    
}
