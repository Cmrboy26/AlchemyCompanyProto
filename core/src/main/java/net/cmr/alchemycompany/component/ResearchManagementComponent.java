package net.cmr.alchemycompany.component;

import java.util.HashMap;
import java.util.HashSet;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.game.Technology;

public class ResearchManagementComponent extends Component {

    HashSet<String> researchedTechnologies;
    Technology currentResearch;
    HashMap<String, Float> resourcesInvested;

    public ResearchManagementComponent() {
        researchedTechnologies = new HashSet<>();
        currentResearch = null;
        resourcesInvested = new HashMap<>();
    }

    @Override
    public void write(Json json) {
        json.writeValue("researchedTechnologies", researchedTechnologies, HashSet.class, Technology.class);
        json.writeValue("currentResearch", currentResearch, String.class);
        
        json.writeArrayStart("resourcesInvested");
        for (String key : resourcesInvested.keySet()) {
            json.writeValue(key, resourcesInvested.get(key), Float.class);
        }
        json.writeArrayEnd();
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        researchedTechnologies = json.readValue("researchedTechnologies", HashSet.class, String.class, jsonData);
        currentResearch = json.readValue("currentResearch", Technology.class, jsonData);

        resourcesInvested = new HashMap<>();
        JsonValue resourcesArray = jsonData.get("resourcesInvested");
        if (resourcesArray != null) {
            for (JsonValue entry = resourcesArray.child; entry != null; entry = entry.next) {
                String key = entry.name;
                Float value = entry.asFloat();
                resourcesInvested.put(key, value);
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
        return researchedTechnologies.contains(technology);
    }
    
}
