package net.cmr.alchemycompany.component;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.game.Effects.AttackType;

public class AttackComponent extends Component {
    
    public float baseAttack;
    public AttackType typeMultipliers;

    public AttackComponent() {}

    public AttackComponent(float baseAttack, AttackType typeMultipliers) {
        this.baseAttack = baseAttack;
        this.typeMultipliers = typeMultipliers;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "baseAttack");
        json.writeField(this, "typeMultipliers");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        json.readValue("baseAttack", Float.class, jsonData);
        json.readValue("typeMultipliers", AttackType.class, jsonData);
    }

}
