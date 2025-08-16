package net.cmr.alchemycompany.system;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.cmr.alchemycompany.component.BuildingComponent;
import net.cmr.alchemycompany.component.Component;
import net.cmr.alchemycompany.component.FogOfWarComponent;
import net.cmr.alchemycompany.component.SightComponent;
import net.cmr.alchemycompany.component.TilePositionComponent;
import net.cmr.alchemycompany.component.UnitComponent;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.ecs.EntitySystem;
import net.cmr.alchemycompany.ecs.Family;
import net.cmr.alchemycompany.world.Distance;
import net.cmr.alchemycompany.world.TilePoint;

public class VisibilitySystem extends EntitySystem {

    public static final int DEFAULT_BUILDING_RADIUS = 3;
    public static final int DEFAULT_TROOP_RADIUS = 3;

    public VisibilitySystem() {

    }

    public boolean isVisibleCurrently(UUID playerID, int x, int y) {
        FogOfWarComponent fogOfWar = engine.getSingletonComponent(FogOfWarComponent.class);
        fogOfWar.currentlyVisibleTiles.putIfAbsent(playerID, new HashSet<>());
        return fogOfWar.currentlyVisibleTiles.get(playerID).contains(new TilePoint(x, y));
    }

    public boolean wasVisiblePreviously(UUID playerID, int x, int y) {
        FogOfWarComponent fogOfWar = engine.getSingletonComponent(FogOfWarComponent.class);
        fogOfWar.previouslyVisibleTiles.putIfAbsent(playerID, new HashSet<>());
        return fogOfWar.previouslyVisibleTiles.get(playerID).contains(new TilePoint(x, y));
    }

    public void updateVisibility(UUID playerID) {
        FogOfWarComponent fogOfWar = engine.getSingletonComponent(FogOfWarComponent.class);
        Family sightFamily = Family.all(SightComponent.class, TilePositionComponent.class);
        Set<Entity> validEntities = engine.getEntities(sightFamily);
        // Keep references valid
        fogOfWar.currentlyVisibleTiles.putIfAbsent(playerID, new HashSet<>());
        fogOfWar.previouslyVisibleTiles.putIfAbsent(playerID, new HashSet<>());
        Set<TilePoint> visibleTileCoordinates = fogOfWar.currentlyVisibleTiles.get(playerID);
        Set<TilePoint> previousVisibleCoordinates = fogOfWar.previouslyVisibleTiles.get(playerID);
        visibleTileCoordinates.clear();
        for (Entity entity : validEntities) {
            TilePositionComponent tilePosition = entity.getComponent(TilePositionComponent.class);
            int radius = entity.getComponent(SightComponent.class).radius;
            for (int x = radius; x >= -radius; x--) {
                for (int y = radius; y >= -radius; y--) {
                    double distance = Distance.manhattan(x, y);
                    if (distance <= radius) {
                        visibleTileCoordinates.add(new TilePoint(tilePosition.tileX + x, tilePosition.tileY + y));
                    }
                }
            }
        }
        previousVisibleCoordinates.addAll(visibleTileCoordinates);
        engine.changedEntity(engine.getSingletonEntity(FogOfWarComponent.class));
    }

}
