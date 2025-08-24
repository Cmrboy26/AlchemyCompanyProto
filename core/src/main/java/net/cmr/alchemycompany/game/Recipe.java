package net.cmr.alchemycompany.game;

import java.util.HashMap;
import java.util.HashSet;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

public class Recipe implements Serializable {
    
    private String id;
    private String name;
    private String description;
    private String icon;
    private HashMap<String, Float> input, output;
    private HashSet<String> requiredTechnologies;
    private int turns;
    
    // Required for JSON deserialization
    public Recipe() {}
    
    public Recipe(String id, String name, String description, String icon, HashMap<String, Float> input, HashMap<String, Float> output, int turns) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.input = input;
        this.output = output;
        this.turns = turns;
        this.requiredTechnologies = new HashSet<>();
    }

    public Recipe(String id, String name, String description, String icon, HashMap<String, Float> input, HashMap<String, Float> output, int turns, String...technologies) {
        this(id, name, description, icon, input, output, turns);
        this.requiredTechnologies = new HashSet<>();
        if (technologies != null) {
            for (String tech : technologies) {
                this.requiredTechnologies.add(tech);
            }
        }
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
    public int getTurns() { return turns; }
    public HashMap<String, Float> getInputs() { return new HashMap<>(input); }
    public HashMap<String, Float> getOutputs() { return new HashMap<>(output); }
    public HashSet<String> getRequiredTechnologies() { return requiredTechnologies; }
    
    @Override
    public void write(Json json) {
        json.writeValue("id", id, String.class);
        json.writeValue("name", name, String.class);
        json.writeValue("description", description, String.class);
        json.writeValue("icon", icon.toString(), String.class);

        // Write input and output as simple object maps to avoid type information
        json.writeObjectStart("input");
        if (input != null) {
            for (String key : input.keySet()) {
                json.writeValue(key, input.get(key));
            }
        }
        json.writeObjectEnd();
        
        json.writeObjectStart("output");
        if (output != null) {
            for (String key : output.keySet()) {
                json.writeValue(key, output.get(key));
            }
        }
        json.writeObjectEnd();
        
        json.writeValue("turns", turns, Integer.class);

        json.writeArrayStart("requiredTechnologies");
        if (requiredTechnologies != null) {
            for (String techId : requiredTechnologies) {
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
        
        // Read input and output manually to avoid type information issues
        this.input = new HashMap<>();
        JsonValue inputData = jsonData.get("input");
        if (inputData != null) {
            for (JsonValue entry = inputData.child; entry != null; entry = entry.next) {
                String key = entry.name;
                
                if (Registry.getResourceRegistry().get(key) == null) {
                    throw new RuntimeException("Resource "+key+" does not exist");
                }

                Float value = entry.asFloat();
                this.input.put(key, value);
            }
        }
        
        this.output = new HashMap<>();
        JsonValue outputData = jsonData.get("output");
        if (outputData != null) {
            for (JsonValue entry = outputData.child; entry != null; entry = entry.next) {
                String key = entry.name;

                if (Registry.getResourceRegistry().get(key) == null) {
                    throw new RuntimeException("Resource "+key+" does not exist");
                }

                Float value = entry.asFloat();
                this.output.put(key, value);
            }
        }
        
        this.turns = json.readValue("turns", Integer.class, 1, jsonData);

        this.requiredTechnologies = new HashSet<>();
        JsonValue techArray = jsonData.get("requiredTechnologies");
        if (techArray != null) {
            for (JsonValue tech = techArray.child; tech != null; tech = tech.next) {
                this.requiredTechnologies.add(tech.asString());
            }
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Recipe recipe = (Recipe) obj;
        return id.equals(recipe.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return "RECIPE." + id;
    }
}
