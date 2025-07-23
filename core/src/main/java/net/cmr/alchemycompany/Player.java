package net.cmr.alchemycompany;

import com.badlogic.gdx.math.Vector2;

import net.cmr.alchemycompany.Building.HeadquarterBuilding;

public class Player {

    String name;
    HeadquarterBuilding hq;

    public Player(String name) {
        this.name = name;
    }

    public void setHQ(HeadquarterBuilding building) {
        this.hq = building;
    }

    public HeadquarterBuilding getHQ() {
        return hq;
    }

    public Vector2 getHQPosition() {
        return new Vector2(hq.getX(), hq.getY());
    }

    public void addBuilding(Building building) {
        if (building instanceof HeadquarterBuilding) {
            setHQ((HeadquarterBuilding) building);
        }
    }

    @Override
    public String toString() {
        return name;
    }

}
