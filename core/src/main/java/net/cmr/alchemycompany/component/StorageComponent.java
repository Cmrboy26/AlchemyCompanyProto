package net.cmr.alchemycompany.component;

import java.util.HashMap;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.game.Resources;
import net.cmr.alchemycompany.game.Resources.Resource;

public class StorageComponent extends Component {

    public HashMap<Resource, Float> storage;
    public HashMap<Resource, Float> maxStorage;

    public StorageComponent() {}

    public StorageComponent(HashMap<Resource, Float> storage, HashMap<Resource, Float> maxStorage) {
        this.storage = storage;
        this.maxStorage = maxStorage;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "storage");
        json.writeField(this, "maxStorage");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        this.storage = Component.readMap(Resource.class, Float.class, new HashMap<>(), jsonData.get("storage"), json);
        if (jsonData.get("maxStorage").size == 0) {
            this.maxStorage = Resources.allItems(jsonData.getFloat("maxStorage"));
        } else {
            this.maxStorage = Component.readMap(Resource.class, Float.class, jsonData.get("maxStorage"), json);
        }
    }
    
}
