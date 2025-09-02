package net.cmr.alchemycompany.game;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

public class Technology implements Serializable {
    
    private String id;
    private String name;
    private String description;
    private String icon;
    private Vector2 position;
    private HashMap<String, Float> cost;
    private HashSet<String> prerequisiteTechnologies;
    
    // Required for JSON deserialization
    public Technology() {}
    
    public Technology(String id, String name, String description, String icon, Vector2 position,
                      HashMap<String, Float> cost,
                      HashSet<String> prerequisiteTechnologies) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.position = position;
        this.cost = cost;
        this.prerequisiteTechnologies = prerequisiteTechnologies;
    }

    public Technology(String id, String name, String description, String icon, Vector2 position,
                      HashMap<String, Float> cost,
                      String... prerequisiteTechnologies) {
        this(id, name, description, icon, position, cost, new HashSet<>());
        if (prerequisiteTechnologies != null) {
            this.prerequisiteTechnologies.addAll(Arrays.asList(prerequisiteTechnologies));
        }
    }
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
    public Vector2 getPosition() { return position == null ? null : new Vector2(position); }
    public HashMap<String, Float> getCost() { return cost == null ? null : new HashMap<>(cost); }
    public HashSet<String> getPrerequisiteTechnologies() { return prerequisiteTechnologies == null ? null : new HashSet<>(prerequisiteTechnologies); }

    @Override
    public void write(Json json) {
        json.writeValue("id", id, String.class);
        json.writeValue("name", name, String.class);
        json.writeValue("description", description, String.class);
        json.writeValue("icon", icon, String.class);

        // Write position as array
        json.writeArrayStart("position");
        json.writeValue(position.x);
        json.writeValue(position.y);
        json.writeArrayEnd();

        // Write cost as object map
        json.writeObjectStart("cost");
        if (cost != null) {
            for (String key : cost.keySet()) {
                json.writeValue(key, cost.get(key));
            }
        }
        json.writeObjectEnd();

        json.writeArrayStart("prerequisiteTechnologies");
        if (prerequisiteTechnologies != null) {
            for (String techId : prerequisiteTechnologies) {
                json.writeValue(techId);
            }
        }
        json.writeArrayEnd();
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        this.id = json.readValue("id", String.class, jsonData);
        this.name = json.readValue("name", String.class, jsonData);
        this.description = json.readValue("description", String.class, jsonData);
        this.icon = json.readValue("icon", String.class, jsonData);

        // Read position as array
        JsonValue posArr = jsonData.get("position");
        if (posArr != null && posArr.isArray() && posArr.size >= 2) {
            float x = posArr.getFloat(0);
            float y = posArr.getFloat(1);
            this.position = new Vector2(x, y);
        } else {
            this.position = new Vector2(-1, -1);
        }

        // Read cost
        this.cost = new HashMap<>();
        JsonValue costData = jsonData.get("cost");
        if (costData != null) {
            for (JsonValue entry = costData.child; entry != null; entry = entry.next) {
                String key = entry.name;
                Float value = entry.asFloat();
                this.cost.put(key, value);
            }
        }

        // Read prerequisiteTechnologies
        this.prerequisiteTechnologies = new HashSet<>();
        JsonValue techArray = jsonData.get("prerequisiteTechnologies");
        if (techArray != null) {
            for (JsonValue tech = techArray.child; tech != null; tech = tech.next) {
                this.prerequisiteTechnologies.add(tech.asString());
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Technology tech = (Technology) obj;
        return id.equals(tech.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "TECHNOLOGY." + id;
    }
}
