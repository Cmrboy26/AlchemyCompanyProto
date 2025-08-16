package net.cmr.alchemycompany.world;

import java.io.Serializable;
import java.util.UUID;

import net.cmr.alchemycompany.world.World.WorldFeature;

public class Tile implements Serializable, Cloneable {

    WorldFeature feature;
    int x, y;
    UUID buildingEntityID;
    UUID troopEntityID;

    public Tile(WorldFeature feature, int x, int y) {
        this.feature = feature;
        this.x = x;
        this.y = y;
    }

    public WorldFeature getFeature() {
        return feature;
    }
    public boolean isBuildingSlotEmpty() {
        return buildingEntityID == null;
    }
    public boolean isTroopSlotEmpty() {
        return troopEntityID == null;
    }
    public boolean canPlaceBuilding() {
        return isBuildingSlotEmpty();
    }
    public boolean canPlaceTroop() {
        return isTroopSlotEmpty();
    }
    public UUID getBuildingSlotID() {
        return buildingEntityID;
    }
    public UUID getTroopSlotID() {
        return troopEntityID;
    }
    public void setBuildingSlotID(UUID id) {
        this.buildingEntityID = id;
    }
    public void setTroopSlotID(UUID id) {
        this.troopEntityID = id;
    }

    @Override
    public Tile clone() {
        try {
            Tile cloned = (Tile) super.clone();
            // UUIDs are immutable, so shallow copy is fine
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

}
