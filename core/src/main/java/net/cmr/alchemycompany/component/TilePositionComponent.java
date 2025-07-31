package net.cmr.alchemycompany.component;

import net.cmr.alchemycompany.ecs.Component;

public class TilePositionComponent extends Component {
    
    public int tileX = 0;
    public int tileY = 0;

    public TilePositionComponent(int tileX, int tileY) {
        this.tileX = tileX;
        this.tileY = tileY;
    } 

}
