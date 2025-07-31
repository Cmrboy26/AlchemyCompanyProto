package net.cmr.alchemycompany.component;

import net.cmr.alchemycompany.ecs.Component;
import net.cmr.alchemycompany.game.Units.UnitType;

public class UnitComponent extends Component {
    
    public UnitType unitType;
    public int equipmentSlots;

    public UnitComponent(UnitType unitType, int equipmentSlots) {
        this.unitType = unitType;
        this.equipmentSlots = equipmentSlots;
    }

}
