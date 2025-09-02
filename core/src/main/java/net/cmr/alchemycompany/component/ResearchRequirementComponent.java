package net.cmr.alchemycompany.component;

import java.util.HashSet;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class ResearchRequirementComponent extends Component {
    
    public HashSet<String> technologiesRequired;

    public ResearchRequirementComponent() {}

    public ResearchRequirementComponent(HashSet<String> technologiesRequired) {
        this.technologiesRequired = technologiesRequired;
    }

    public ResearchRequirementComponent(String technologyRequired) {
        HashSet<String> techSet = new HashSet<>();
        techSet.add(technologyRequired);
        this.technologiesRequired = techSet;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "technologiesRequired");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        this.technologiesRequired = json.readValue(HashSet.class, String.class, jsonData.get("technologiesRequired"));
    }

}
