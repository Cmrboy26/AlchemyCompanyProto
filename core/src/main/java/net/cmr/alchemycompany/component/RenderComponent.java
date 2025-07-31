package net.cmr.alchemycompany.component;

import net.cmr.alchemycompany.Sprites.SpriteType;
import net.cmr.alchemycompany.ecs.Component;

public class RenderComponent extends Component {
    
    public SpriteType spriteType;
    public boolean invertable = false;

    public RenderComponent(SpriteType spriteType) {
        this.spriteType = spriteType;
    }
    public RenderComponent(SpriteType spriteType, boolean invertable) {
        this.spriteType = spriteType;
        this.invertable = invertable;
    }

}
