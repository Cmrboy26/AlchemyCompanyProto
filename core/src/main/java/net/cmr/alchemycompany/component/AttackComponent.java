package net.cmr.alchemycompany.component;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.game.Effects.AttackType;

public class AttackComponent extends Component {
    
    public float baseAttack;
    public AttackType attackType;

    public AttackComponent() {}

    public AttackComponent(float baseAttack, AttackType attackType) {
        this.baseAttack = baseAttack;
        this.attackType = attackType;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "baseAttack");
        json.writeField(this, "attackType");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        this.baseAttack = json.readValue("baseAttack", Float.class, jsonData);
        this.attackType = json.readValue("attackType", AttackType.class, jsonData);
    }

}
