package net.cmr.alchemycompany.entity;

import java.util.Objects;

import net.cmr.alchemycompany.component.TilePositionComponent;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.world.TilePoint;

public class EntityUtils {
    
    public static TilePoint getPosition(Entity entity) {
        TilePositionComponent tpc = entity.getComponent(TilePositionComponent.class);
        Objects.requireNonNull(tpc);
        return new TilePoint(tpc.tileX, tpc.tileY);
    }

}
