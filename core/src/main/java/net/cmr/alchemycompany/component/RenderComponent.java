package net.cmr.alchemycompany.component;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.Sprites.RenderType;

public class RenderComponent extends Component {
    
    public String renderId;
    public String renderType;
    public boolean invertable = false;
    public transient float elapsedTime = 0;
    public Map<String, String> variants;

    public RenderComponent() {}

    @Override
    public void write(Json json) {
        json.writeValue("renderId", renderId, String.class);
        json.writeValue("renderType", renderType, String.class);
        json.writeValue("invertable", invertable, Boolean.class);
        //json.writeValue("elapsedTime", elapsedTime, Float.class);
        json.writeField(this, "variants", Map.class);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        String finalRenderId = json.readValue("renderId", String.class, jsonData);
        String finalRenderType = json.readValue("renderType", String.class, jsonData);
        if (finalRenderId == null || finalRenderType == null) {
            finalRenderId = json.readValue("spriteType", String.class, jsonData);
            finalRenderType = "SPRITE";
        }
        if (finalRenderId == null || finalRenderType == null) {
            finalRenderId = json.readValue("animationType", String.class, jsonData);
            finalRenderType = "ANIMATION";
        }
        this.renderId = finalRenderId;
        this.renderType = finalRenderType;
        this.invertable = json.readValue("invertable", Boolean.class, false, jsonData);
        //this.elapsedTime = json.readValue("elapsedTime", Float.class, 0f, jsonData);
        this.variants = json.readValue("variants", HashMap.class, String.class, jsonData);
    }

    public RenderType getRenderType() {
        return RenderType.valueOf(renderType);
    }

}
