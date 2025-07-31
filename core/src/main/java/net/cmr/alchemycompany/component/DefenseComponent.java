package net.cmr.alchemycompany.component;

import java.util.Map;

import net.cmr.alchemycompany.ecs.Component;
import net.cmr.alchemycompany.game.Effects.AttackType;

public class DefenseComponent extends Component {
    
    public float baseDefense;
    public Map<AttackType, Float> typeMultipliers;

    public DefenseComponent(float baseDefense, Map<AttackType,Float> typeMultipliers) {
        this.baseDefense = baseDefense;
        this.typeMultipliers = typeMultipliers;
    }

}
