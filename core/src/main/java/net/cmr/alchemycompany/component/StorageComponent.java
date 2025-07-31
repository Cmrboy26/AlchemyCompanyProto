package net.cmr.alchemycompany.component;

import java.util.Map;

import net.cmr.alchemycompany.ecs.Component;
import net.cmr.alchemycompany.game.Resources.Resource;

public class StorageComponent extends Component {

    public Map<Resource, Float> storage;
    public Map<Resource, Float> maxStorage;

    public StorageComponent(Map<Resource, Float> storage, Map<Resource, Float> maxStorage) {
        this.storage = storage;
        this.maxStorage = maxStorage;
    }
    
}
