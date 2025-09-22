package net.cmr.alchemycompany.component;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class UnitComponent extends Component {
    
    public String unitId;
    public int equipmentSlots;

    public UnitComponent() {}

    public UnitComponent(String unitId, int equipmentSlots) {
        this.unitId = unitId;
        this.equipmentSlots = equipmentSlots;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "unitId");
        json.writeField(this, "equipmentSlots");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        this.unitId = json.readValue("unitId", String.class, jsonData);
        this.equipmentSlots = json.readValue("equipmentSlots", Integer.class, jsonData);
    }

}
