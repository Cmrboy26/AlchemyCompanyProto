package net.cmr.alchemycompany.component;

import java.util.UUID;

import net.cmr.alchemycompany.ecs.Component;

public class OwnerComponent extends Component {

    public UUID playerID;

    public OwnerComponent(UUID playerID) {
        this.playerID = playerID;
    }
    
}
