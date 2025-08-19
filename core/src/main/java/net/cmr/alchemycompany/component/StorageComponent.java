package net.cmr.alchemycompany.component;

import java.util.HashMap;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.game.Resources;

public class StorageComponent extends Component {

    public HashMap<String, Float> storage;
    public HashMap<String, Float> maxStorage;

    public StorageComponent() {
        this.storage = new HashMap<>();
        this.maxStorage = new HashMap<>();
    }

    public StorageComponent(HashMap<String, Float> storage, HashMap<String, Float> maxStorage) {
        this.storage = storage;
        this.maxStorage = maxStorage;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "storage");
        if (maxStorage.size() == Resources.allItems(1f, false).size()) {
            float size = maxStorage.get(maxStorage.keySet().iterator().next());
            json.writeValue("maxStorage", size, Float.class);
        } else {
            json.writeField(this, "maxStorage");
        }
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        json.readField(this, "storage", jsonData);
        if (jsonData.get("maxStorage") == null || jsonData.get("maxStorage").size == 0) {
            this.maxStorage = Resources.allItems(jsonData.getFloat("maxStorage"), false);
        } else {
            json.readField(this, "maxStorage", jsonData);
        }
    }
    
}
