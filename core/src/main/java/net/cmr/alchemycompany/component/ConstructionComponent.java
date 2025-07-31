package net.cmr.alchemycompany.component;

import net.cmr.alchemycompany.ecs.Component;

public class ConstructionComponent extends Component {

    public int turns;

    public ConstructionComponent(int turns) {
        this.turns = turns;
    }
    
}
