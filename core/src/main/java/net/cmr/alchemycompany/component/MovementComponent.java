package net.cmr.alchemycompany.component;
import net.cmr.alchemycompany.ecs.Component;

public class MovementComponent extends Component {
    
    public float movesPerTurn, movesRemaining;

    public MovementComponent(float movesPerTurn, float movesRemaining) {
        this.movesPerTurn = movesPerTurn;
        this.movesRemaining = movesRemaining;
    }

}
