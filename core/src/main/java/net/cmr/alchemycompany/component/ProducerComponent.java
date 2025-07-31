package net.cmr.alchemycompany.component;

import java.util.Map;

import net.cmr.alchemycompany.ecs.Component;
import net.cmr.alchemycompany.game.Resources.Resource;

public class ProducerComponent extends Component {

    public Map<Resource, Float> production;
    
    public ProducerComponent(Map<Resource, Float> production) {
        this.production = production;
    }


}
