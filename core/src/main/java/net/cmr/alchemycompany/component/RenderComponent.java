package net.cmr.alchemycompany.component;

import net.cmr.alchemycompany.Sprites.SpriteType;
import net.cmr.alchemycompany.ecs.Component;

public class RenderComponent extends Component {
    
    public SpriteType spriteType;

    public RenderComponent(SpriteType spriteType) {
        this.spriteType = spriteType;
    }

}
