package net.cmr.alchemycompany.component.actions;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.component.Component;

public class ResearchActionComponent extends Component implements IActionComponent {

    private String researchId;
    private boolean starts; // true = start research, false = stop research

    public ResearchActionComponent() { }
    public ResearchActionComponent(String researchId, boolean starts) {
        this.researchId = researchId;
        this.starts = starts;
    }   
    public ResearchActionComponent(String researchId) {
        this(researchId, true);
    }

    @Override
    public void write(Json json) {
        json.writeValue("researchId", researchId);
        json.writeValue("starts", starts);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        researchId = jsonData.getString("researchId");
        starts = jsonData.getBoolean("starts", true);
    }

    public String getResearchId() {
        return researchId;
    }
    public boolean isStarts() {
        return starts;
    }

}
