package net.cmr.alchemycompany.component;

import java.util.HashSet;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.game.Technologies.Technology;

public class ResearchRequirementComponent extends Component {
    
    public HashSet<Technology> technologiesRequired;

    public ResearchRequirementComponent() {}

    public ResearchRequirementComponent(HashSet<Technology> technologiesRequired) {
        this.technologiesRequired = technologiesRequired;
    }

    public ResearchRequirementComponent(Technology technologyRequired) {
        HashSet<Technology> techSet = new HashSet<>();
        techSet.add(technologyRequired);
        this.technologiesRequired = techSet;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "technologiesRequired");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        this.technologiesRequired = json.readValue(HashSet.class, Technology.class, jsonData.get("technologiesRequired"));
    }

}
