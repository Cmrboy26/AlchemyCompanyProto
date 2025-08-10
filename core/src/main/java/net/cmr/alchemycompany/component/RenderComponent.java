package net.cmr.alchemycompany.component;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.Sprites.SpriteType;

public class RenderComponent extends Component {
    
    public SpriteType spriteType;
    public boolean invertable = false;

    public RenderComponent() {}

    public RenderComponent(SpriteType spriteType) {
        this.spriteType = spriteType;
    }
    public RenderComponent(SpriteType spriteType, boolean invertable) {
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
        this.spriteType = json.readValue("spriteType", SpriteType.class, jsonData);
        this.invertable = json.readValue("invertable", Boolean.class, false, jsonData);
    }

}
