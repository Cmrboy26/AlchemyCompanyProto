package net.cmr.alchemycompany.component;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.game.Equipment;

public class EquipmentComponent extends Component {
    
    public Equipment equipment;

    public EquipmentComponent() {}

    @Override
    public void write(Json json) {
        json.writeField(this, "equipment");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        this.equipment = json.readValue("equipment", Equipment.class, jsonData);
    }

}
