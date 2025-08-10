package net.cmr.alchemycompany.component;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.game.Units.UnitType;

public class UnitComponent extends Component {
    
    public UnitType unitType;
    public int equipmentSlots;

    public UnitComponent() {}

    public UnitComponent(UnitType unitType, int equipmentSlots) {
        this.unitType = unitType;
        this.equipmentSlots = equipmentSlots;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "unitType");
        json.writeField(this, "equipmentSlots");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        this.unitType = json.readValue("unitType", UnitType.class, jsonData);
        this.equipmentSlots = json.readValue("equipmentSlots", Integer.class, jsonData);
    }

}
