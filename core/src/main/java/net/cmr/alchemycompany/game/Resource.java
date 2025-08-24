package net.cmr.alchemycompany.game;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Stores global information about a resource for use in certain logic systems.
 */
public class Resource implements Serializable {
    
    private String id;
    private String name;
    private String description;
    private String icon;
    /**
     * Resources where this is true aren't stored in the world. They are immediately used whenever a building or system needs it, and excess is discarded.
     * Example resources are science and, in other 4X games, culture.
     */
    private boolean perTurnResource;
    
    // Required for JSON deserialization
    public Resource() {}
    
    public Resource(String id, String name, String description, String icon, boolean perTurnResource) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.perTurnResource = perTurnResource;
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
    public boolean isPerTurnResource() { return perTurnResource; }
    
    @Override
    public void write(Json json) {
        json.writeValue("id", id, String.class);
        json.writeValue("name", name, String.class);
        json.writeValue("description", description, String.class);
        json.writeValue("icon", icon.toString(), String.class);
        json.writeValue("perTurnResource", perTurnResource, Boolean.class);
    }
    
    @Override
    public void read(Json json, JsonValue jsonData) {
        this.id = json.readValue("id", String.class, jsonData);
        this.name = json.readValue("name", String.class, jsonData);
        this.description = json.readValue("description", String.class, jsonData);
        this.icon = json.readValue("icon", String.class, jsonData);
        this.perTurnResource = json.readValue("perTurnResource", Boolean.class, false, jsonData);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Resource resource = (Resource) obj;
        return id.equals(resource.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return "RESOURCE." + id;
    }
}
