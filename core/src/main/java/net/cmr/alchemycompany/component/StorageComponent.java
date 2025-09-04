package net.cmr.alchemycompany.component;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.game.Resource;
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

    public Map<String, Float> getStorage() {
        return storage;
    }
    public Map<String, Float> getMaxStorage() {
        return maxStorage;
    }
    public Float getMaxStorage(String resource) {
        return maxStorage.getOrDefault(resource, 0f);
    }
    
    public float getAmountStored(String resource) {
        return storage.getOrDefault(resource, 0f);
    }

    protected void setAmountStored(String resource, float amount) {
        storage.put(resource, amount);
        if (amount > getMaxStorage(resource)) {
            throw new RuntimeException("Stored more than maximum amount in storage building: (" + amount + " > " + getMaxStorage(resource) + ")");
        }
    }
    /**
     * @return amount of resource that cannot be added
     */
    public float addAmount(String resource, float amount) {
        if (amount < 0) {
            throw new RuntimeException("Cannot add negative amount in addAmount method");
        }
        if (getAmountStored(resource) + amount > getMaxStorage(resource)) {
            float amountToReturn = getAmountStored(resource) + amount - getMaxStorage(resource);
            setAmountStored(resource, getMaxStorage(resource));
            return amountToReturn;
        }
        setAmountStored(resource, getAmountStored(resource) + amount);
        return 0;
    }

    public void consumeAmount(String resource, float amount) {
        if (amount < 0) {
            throw new RuntimeException("Cannot consume negative amount in consumeAmount method");
        }
        setAmountStored(resource, getAmountStored(resource) - amount);
    }
    
}
