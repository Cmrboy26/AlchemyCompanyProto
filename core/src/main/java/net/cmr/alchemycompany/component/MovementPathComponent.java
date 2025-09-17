package net.cmr.alchemycompany.component;

import java.util.LinkedList;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.system.MovementSystem.MovementPath;
import net.cmr.alchemycompany.world.TilePoint;

public class MovementPathComponent extends Component {

    public MovementPath movementPath;
    public transient float elapsedTime = 0;

    public MovementPathComponent() { }
    public MovementPathComponent(MovementPath movementPath) {
        this.movementPath = movementPath;
    }

    @Override
    public void write(Json json) {
        json.writeArrayStart("movementPath");
        for (TilePoint tilePoint : movementPath.getMovementPath()) {
            json.writeArrayStart();
            json.writeValue(tilePoint.getX(), Integer.class);
            json.writeValue(tilePoint.getY(), Integer.class);
            json.writeArrayEnd();
        }
        json.writeArrayEnd();
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        LinkedList<TilePoint> tp = new LinkedList<>();
        for (JsonValue entry = jsonData.get("movementPath").child; entry != null; entry = entry.next) {
            int x = entry.child.asInt();
            int y = entry.child.next.asInt();
            TilePoint tilePoint = new TilePoint(x, y);
            tp.add(tilePoint);
        }
        MovementPath movementPath = MovementPath.fromPath(tp);
        this.movementPath = movementPath;
    }

    public boolean isFinished(float speed) {
        return Math.floor(elapsedTime * speed) >= movementPath.getMovementPath().size() - 1;
    }

    public TilePoint getVisiualPosition(float speed) {
        int index = (int) Math.floor(elapsedTime * speed);
        index = Math.min(index, movementPath.getMovementPath().size() - 1);
        TilePoint currentTile = movementPath.getMovementPath().get(index);
        return new TilePoint(currentTile.getX(), currentTile.getY());
    }

    public Vector2 getOffsetVisualPosition(float speed) {
        int index = (int) Math.floor(elapsedTime * speed);
        TilePoint finalTile = movementPath.getMovementPath().get(movementPath.getMovementPath().size() - 1);
        Vector2 finalTileVector = new Vector2(finalTile.getX(), finalTile.getY());
        if (index >= movementPath.getMovementPath().size() - 1) {
            return new Vector2(0, 0);
        }
        index = Math.min(index, movementPath.getMovementPath().size() - 2);
        TilePoint currentTile = movementPath.getMovementPath().get(index);
        Vector2 currentTileVector = new Vector2(currentTile.getX(), currentTile.getY());
        TilePoint nextTile = movementPath.getMovementPath().get(index + 1);
        Vector2 directionVector = new Vector2(nextTile.getX() - currentTileVector.x, nextTile.getY() - currentTileVector.y);
        directionVector.scl((elapsedTime * speed) % 1.0f);
        Vector2 finalVector = new Vector2();
        finalVector.add(directionVector);
        return finalVector;
    }
    
}
