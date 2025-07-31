package net.cmr.alchemycompany.component;

import net.cmr.alchemycompany.ecs.Component;

public class ScienceComponent extends Component {
    
    public float sciencePerTurn;

    public ScienceComponent(float sciencePerTurn) {
        this.sciencePerTurn = sciencePerTurn;
    }

}
