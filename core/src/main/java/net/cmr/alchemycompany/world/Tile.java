package net.cmr.alchemycompany.world;

import java.util.UUID;

import net.cmr.alchemycompany.world.World.WorldFeature;

public class Tile implements Cloneable {

    WorldFeature feature;
    int x, y;
    UUID buildingEntityID;
    UUID unitEntityID;
    private transient World world;

    public Tile() { }

    public Tile(World world, WorldFeature feature, int x, int y) {
        this.world = world;
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
    public boolean isUnitSlotEmpty() {
        return unitEntityID == null;
    }
    public boolean canPlaceBuilding() {
        return isBuildingSlotEmpty();
    }
    public boolean canPlaceUnit() {
        return isUnitSlotEmpty();
    }
    public UUID getBuildingSlotID() {
        return buildingEntityID;
    }
    public UUID getUnitSlotID() {
        return unitEntityID;
    }
    public void setBuildingSlotID(UUID id) {
        this.buildingEntityID = id;
        world.onTileChange(this);
    }
    public void setUnitSlotID(UUID id) {
        this.unitEntityID = id;
        world.onTileChange(this);
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
