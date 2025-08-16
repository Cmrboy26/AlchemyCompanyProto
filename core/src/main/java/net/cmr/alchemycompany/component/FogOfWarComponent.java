package net.cmr.alchemycompany.component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.world.TilePoint;

public class FogOfWarComponent extends Component {

    public Map<UUID, Set<TilePoint>> currentlyVisibleTiles;
    public Map<UUID, Set<TilePoint>> previouslyVisibleTiles;

    public FogOfWarComponent() {
        currentlyVisibleTiles = new HashMap<>();
        previouslyVisibleTiles = new HashMap<>();
    }

    @Override
    public void write(Json json) {
        // Convert UUID keys to String for serialization
        Map<String, Set<TilePoint>> currentStrMap = new HashMap<>();
        for (Map.Entry<UUID, Set<TilePoint>> entry : currentlyVisibleTiles.entrySet()) {
            currentStrMap.put(entry.getKey().toString(), entry.getValue());
        }
        Map<String, Set<TilePoint>> previousStrMap = new HashMap<>();
        for (Map.Entry<UUID, Set<TilePoint>> entry : previouslyVisibleTiles.entrySet()) {
            previousStrMap.put(entry.getKey().toString(), entry.getValue());
        }
        json.writeValue("currentlyVisibleTiles", currentStrMap, Map.class, Set.class);
        json.writeValue("previouslyVisibleTiles", previousStrMap, Map.class, Set.class);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        // Read maps with String keys, convert back to UUID
        Map<String, Set<TilePoint>> currentStrMap = json.readValue("currentlyVisibleTiles", Map.class, Set.class, jsonData);
        Map<String, Set<TilePoint>> previousStrMap = json.readValue("previouslyVisibleTiles", Map.class, Set.class, jsonData);

        currentlyVisibleTiles = new HashMap<>();
        if (currentStrMap != null) {
            for (Map.Entry<String, Set<TilePoint>> entry : currentStrMap.entrySet()) {
                currentlyVisibleTiles.put(UUID.fromString(entry.getKey()), entry.getValue());
            }
        }

        previouslyVisibleTiles = new HashMap<>();
        if (previousStrMap != null) {
            for (Map.Entry<String, Set<TilePoint>> entry : previousStrMap.entrySet()) {
                previouslyVisibleTiles.put(UUID.fromString(entry.getKey()), entry.getValue());
            }
        }
    }

}
