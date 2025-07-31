package net.cmr.alchemycompany.component;

import net.cmr.alchemycompany.ecs.Component;
import net.cmr.alchemycompany.game.Effects.AttackType;

public class AttackComponent extends Component {
    
    public float baseAttack;
    public AttackType baseType;

    public AttackComponent(float baseAttack, AttackType baseType) {
        this.baseAttack = baseAttack;
        this.baseType = baseType;
    }

}
