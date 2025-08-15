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
        json.writeValue("currentlyVisibleTiles", currentlyVisibleTiles, Map.class, Set.class);
        json.writeValue("previouslyVisibleTiles", previouslyVisibleTiles, Map.class, Set.class);
    }
    @Override
    public void read(Json json, JsonValue jsonData) {
        currentlyVisibleTiles = json.readValue("currentlyVisibleTiles", Map.class, Set.class, jsonData);
        previouslyVisibleTiles = json.readValue("previouslyVisibleTiles", Map.class, Set.class, jsonData);
    }

}
