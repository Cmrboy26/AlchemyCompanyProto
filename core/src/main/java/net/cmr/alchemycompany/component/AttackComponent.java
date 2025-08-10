package net.cmr.alchemycompany.component;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.game.Effects.AttackType;

public class AttackComponent extends Component {
    
    public float baseAttack;
    public AttackType baseType;

    public AttackComponent() {}

    public AttackComponent(float baseAttack, AttackType baseType) {
        this.baseAttack = baseAttack;
        this.baseType = baseType;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "baseAttack");
        json.writeField(this, "baseType");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        json.readValue("baseAttack", Float.class, jsonData);
        json.readValue("baseType", AttackType.class, jsonData);
    }

}
