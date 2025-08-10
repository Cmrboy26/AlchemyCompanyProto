package net.cmr.alchemycompany.component;

import java.util.HashMap;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.game.Effects.AttackType;
import net.cmr.alchemycompany.game.Resources.Resource;

public class DefenseComponent extends Component {
    
    public float baseDefense;
    public HashMap<AttackType, Float> typeMultipliers;

    public DefenseComponent() {}

    public DefenseComponent(float baseDefense, HashMap<AttackType,Float> typeMultipliers) {
        this.baseDefense = baseDefense;
        this.typeMultipliers = typeMultipliers;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "baseDefense");
        json.writeField(this, "typeMultipliers");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        this.baseDefense = json.readValue("baseDefense", Float.class, jsonData);
        this.typeMultipliers = Component.readMap(AttackType.class, Float.class, jsonData.get("typeMultipliers"), json);
    }

}
