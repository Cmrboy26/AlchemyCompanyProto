package net.cmr.alchemycompany.system;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.cmr.alchemycompany.component.FogOfWarComponent;
import net.cmr.alchemycompany.component.OwnerComponent;
import net.cmr.alchemycompany.component.SightComponent;
import net.cmr.alchemycompany.component.TilePositionComponent;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.ecs.EntitySystem;
import net.cmr.alchemycompany.ecs.Family;
import net.cmr.alchemycompany.world.Distance;
import net.cmr.alchemycompany.world.TilePoint;

public class VisibilitySystem extends EntitySystem {

    public static final int DEFAULT_BUILDING_RADIUS = 3;
    public static final int DEFAULT_UNIT_RADIUS = 2;

    private Family fogOfWarFamily = Family.all(FogOfWarComponent.class, OwnerComponent.class);

    public VisibilitySystem() {

    }

    private Entity getFogHolderEntity(UUID playerUUID) {
        Set<Entity> fogEntities = engine.getEntities(fogOfWarFamily);
        for (Entity entity : fogEntities) {
            OwnerComponent owner = entity.getComponent(OwnerComponent.class);
            if (owner != null && owner.getUUID() != null && owner.getUUID().equals(playerUUID)) {
                return entity;
            }
        }
        return null;
    }

    private FogOfWarComponent getFogComponent(UUID playerUUID) {
        Entity fogEntity = getFogHolderEntity(playerUUID);
        if (fogEntity == null) {
            throw new IllegalStateException("No FogOfWarComponent found for player " + playerUUID);
        }
        FogOfWarComponent fogOfWar = fogEntity.getComponent(FogOfWarComponent.class);
        if (fogOfWar == null) {
            throw new IllegalStateException("No FogOfWarComponent found for player " + playerUUID);
        }
        return fogOfWar;
    }

    public boolean isVisibleAnywhere(Collection<UUID> playerUUIDs, int x, int y) {
        for (UUID uuid : playerUUIDs) {
            if (isVisibleCurrently(uuid, x, y)) { return true; }
        }
        return false;
    }

    public boolean isVisibleCurrently(UUID playerUUID, int x, int y) {
        FogOfWarComponent fogOfWar = getFogComponent(playerUUID);
        fogOfWar.currentlyVisibleTiles.putIfAbsent(playerUUID, new HashSet<>());
        return fogOfWar.currentlyVisibleTiles.get(playerUUID).contains(new TilePoint(x, y));
    }

    public boolean wasVisiblePreviously(UUID playerUUID, int x, int y) {
        FogOfWarComponent fogOfWar = getFogComponent(playerUUID);
        fogOfWar.previouslyVisibleTiles.putIfAbsent(playerUUID, new HashSet<>());
        return fogOfWar.previouslyVisibleTiles.get(playerUUID).contains(new TilePoint(x, y));
    }

    public void updateVisibility(UUID playerUUID) {
        FogOfWarComponent fogOfWar = getFogComponent(playerUUID);
        Family sightFamily = Family.all(SightComponent.class, TilePositionComponent.class, OwnerComponent.class);
        Set<Entity> validEntities = engine.getEntities(sightFamily);
        // Keep references valid
        fogOfWar.currentlyVisibleTiles.putIfAbsent(playerUUID, new HashSet<>());
        fogOfWar.previouslyVisibleTiles.putIfAbsent(playerUUID, new HashSet<>());
        Set<TilePoint> visibleTileCoordinates = fogOfWar.currentlyVisibleTiles.get(playerUUID);
        Set<TilePoint> previousVisibleCoordinates = fogOfWar.previouslyVisibleTiles.get(playerUUID);
        visibleTileCoordinates.clear();
        for (Entity entity : validEntities) {
            TilePositionComponent tilePosition = entity.getComponent(TilePositionComponent.class);
            OwnerComponent owner = entity.getComponent(OwnerComponent.class);
            if (!owner.getUUID().equals(playerUUID)) {
                continue;
            }
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
        //engine.changedEntity(getFogHolderEntity(playerUUID));
        engine.changedComponent(getFogHolderEntity(playerUUID), FogOfWarComponent.class);
    }

}
