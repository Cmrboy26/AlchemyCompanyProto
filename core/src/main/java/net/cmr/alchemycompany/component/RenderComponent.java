package net.cmr.alchemycompany.component;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class RenderComponent extends Component {
    
    public String spriteType;
    public boolean invertable = false;

    public RenderComponent() {}

    public RenderComponent(String spriteType) {
        this.spriteType = spriteType;
    }
    public RenderComponent(String spriteType, boolean invertable) {
        this.spriteType = spriteType;
        this.invertable = invertable;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "spriteType");
        json.writeField(this, "invertable");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        this.spriteType = json.readValue("spriteType", String.class, jsonData);
        this.invertable = json.readValue("invertable", Boolean.class, false, jsonData);
    }

}
