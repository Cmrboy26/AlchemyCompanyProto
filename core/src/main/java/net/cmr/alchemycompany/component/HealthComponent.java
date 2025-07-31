package net.cmr.alchemycompany.component;

import net.cmr.alchemycompany.ecs.Component;

public class HealthComponent extends Component {
    
    public float health, maxHealth;

    public HealthComponent(float maxHealth) {
        this.health = maxHealth;
        this.maxHealth = maxHealth;
    }

}
