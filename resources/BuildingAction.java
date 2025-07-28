package net.cmr.alchemycompany;

import java.util.function.Consumer;

public class BuildingAction {
    
    int turnsRemaining;
    Consumer<Building> onFinishEvent;
    private Building building;

    public BuildingAction(int turns, Consumer<Building> onFinish) {
        this.turnsRemaining = turns;
        this.onFinishEvent = onFinish;
    }

    public void setBuilding(Building building) {
        this.building = building;
    }

    public int getTurnsRemaining() {
        return turnsRemaining;
    }

    public boolean step() {
        turnsRemaining--;
        if (turnsRemaining == 0) {
            onFinishEvent.accept(building);
            return true;
        }
        return false;
    }

    public static class ConstructAction extends BuildingAction {
        public ConstructAction(int turns) {
            super(turns, (b -> {
                if (!b.destroyed) {
                    b.built = true;
                }
            }));
        }
    }

}
