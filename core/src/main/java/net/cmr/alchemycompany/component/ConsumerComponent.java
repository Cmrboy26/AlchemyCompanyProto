package net.cmr.alchemycompany.component;

import java.util.Map;

import net.cmr.alchemycompany.ecs.Component;
import net.cmr.alchemycompany.game.Resources.Resource;

public class ConsumerComponent extends Component {

    public Map<Resource, Float> consumption;
    
    public ConsumerComponent(Map<Resource, Float> consumption) {
        this.consumption = consumption;
    }


}
