package net.cmr.alchemycompany.component;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class HealthComponent extends Component {
    
    public Float health, maxHealth;

    public HealthComponent() {}

    public HealthComponent(float maxHealth) {
        this.health = maxHealth;
        this.maxHealth = maxHealth;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "maxHealth");
        json.writeField(this, "health");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        this.maxHealth = json.readValue("maxHealth", Float.class, jsonData);
        this.health = json.readValue("health", Float.class, jsonData);
        if (this.health == null) {
            this.health = maxHealth;
        }
        System.out.println(maxHealth + ", "+ health);
    }  

}
