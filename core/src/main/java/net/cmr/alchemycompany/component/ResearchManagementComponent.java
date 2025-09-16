package net.cmr.alchemycompany.component;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.game.Registry;
import net.cmr.alchemycompany.game.Technology;

public class ResearchManagementComponent extends Component {

    HashSet<String> researchedTechnologies;
    String currentResearchID;
    HashMap<String, Float> costRemaining;

    public ResearchManagementComponent() {
        researchedTechnologies = new HashSet<>();
        currentResearchID = null;
        costRemaining = new HashMap<>();
    }

    @Override
    public void write(Json json) {
        json.writeValue("researchedTechnologies", researchedTechnologies, HashSet.class, String.class);
        json.writeValue("currentResearch", currentResearchID, String.class);
        
        json.writeObjectStart("costRemaining");
        for (String key : costRemaining.keySet()) {
            System.out.println(key + " : "+costRemaining.get(key));
            json.writeValue(key, costRemaining.get(key), Float.class);
        }
        json.writeObjectEnd();
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        researchedTechnologies = json.readValue("researchedTechnologies", HashSet.class, String.class, jsonData);
        currentResearchID = json.readValue("currentResearch", String.class, jsonData);

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
        return currentResearchID != null;
    }
    public Technology getCurrentResearch() {
        return Registry.getInstance().getRegistry(Technology.class).get(currentResearchID);
    }
    public String getCurrentResearchID() {
        return currentResearchID;
    }
    public boolean hasResearched(String technology) {
        //return true;
        return researchedTechnologies.contains(technology);
    }
    public boolean hasResearched(Collection<String> technologies) {
        for (String tech : technologies) {
            if (!hasResearched(tech)) return false;
        }
        return true;
    }
    public void setResearched(String technologyId) {
        researchedTechnologies.add(technologyId);
    }
    public boolean prerequisitesMet(String technology) {
        for (String prereq : Registry.getInstance().getRegistry(Technology.class).get(technology).getPrerequisiteTechnologies()) {
            if (!hasResearched(prereq)) {
                return false;
            }
        }
        return true;
    }
    public HashMap<String, Float> getCostRemaining() {
        if (isResearching()) {
            return new HashMap<>(costRemaining);
        }
        return new HashMap<>();
    }
    public void setCostRemaining(HashMap<String, Float> costRemaining) {
        this.costRemaining = costRemaining;
    }
    public HashMap<String, Float> getCostConsumed() {
        if (isResearching()) {
            HashMap<String, Float> newMap = new HashMap<>();
            for (String resourceID : getCurrentResearch().getCost().keySet()) {
                newMap.put(resourceID, getCurrentResearch().getCost().get(resourceID) - costRemaining.get(resourceID));
            }
            return newMap;
        }
        return new HashMap<>();
    }
    public boolean startResearch(String technologyId) {
        if (getCurrentResearch() != null) return false; // Already researching something
        Technology tech = Registry.getInstance().getRegistry(Technology.class).get(technologyId);
        if (tech == null) return false; // Invalid technology
        if (researchedTechnologies.contains(technologyId)) return false; // Already researched
        currentResearchID = technologyId;
        costRemaining = new HashMap<>(tech.getCost());
        return true;
    }
    public void stopResearch() {
        currentResearchID = null;
        costRemaining.clear();
    }
    
}
