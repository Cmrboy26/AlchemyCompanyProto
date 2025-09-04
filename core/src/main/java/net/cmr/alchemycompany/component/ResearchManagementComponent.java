package net.cmr.alchemycompany.component;

import java.util.HashMap;
import java.util.HashSet;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.game.Registry;
import net.cmr.alchemycompany.game.Technology;

public class ResearchManagementComponent extends Component {

    HashSet<String> researchedTechnologies;
    Technology currentResearch;
    HashMap<String, Float> costRemaining;

    public ResearchManagementComponent() {
        researchedTechnologies = new HashSet<>();
        currentResearch = null;
        costRemaining = new HashMap<>();
    }

    @Override
    public void write(Json json) {
        json.writeValue("researchedTechnologies", researchedTechnologies, HashSet.class, Technology.class);
        json.writeValue("currentResearch", currentResearch, String.class);
        
        json.writeArrayStart("costRemaining");
        for (String key : costRemaining.keySet()) {
            json.writeValue(key, costRemaining.get(key), Float.class);
        }
        json.writeArrayEnd();
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        researchedTechnologies = json.readValue("researchedTechnologies", HashSet.class, String.class, jsonData);
        currentResearch = json.readValue("currentResearch", Technology.class, jsonData);

        costRemaining = new HashMap<>();
        JsonValue resourcesArray = jsonData.get("costRemaining");
        if (resourcesArray != null) {
            for (JsonValue entry = resourcesArray.child; entry != null; entry = entry.next) {
                String key = entry.name;
                Float value = entry.asFloat();
                costRemaining.put(key, value);
            }
        }
    }

    public boolean isResearching() {
        return currentResearch != null;
    }
    public Technology getCurrentResearch() {
        return currentResearch;
    }
    public boolean hasResearched(String technology) {
        //return true;
        return researchedTechnologies.contains(technology);
    }
    public boolean startResearch(String technologyId) {
        if (currentResearch != null) return false; // Already researching something
        Technology tech = Registry.getInstance().getRegistry(Technology.class).get(technologyId);
        if (tech == null) return false; // Invalid technology
        if (researchedTechnologies.contains(technologyId)) return false; // Already researched
        currentResearch = tech;
        costRemaining = new HashMap<>(tech.getCost());
        return true;
    }
    public void stopResearch() {
        currentResearch = null;
        costRemaining.clear();
    }
    
}
