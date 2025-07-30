package net.cmr.alchemycompany.component;

import net.cmr.alchemycompany.ecs.Component;
import net.cmr.alchemycompany.entity.BuildingFactory.BuildingType;

public class BuildingComponent extends Component {

    public BuildingType buildingType;

    public BuildingComponent(BuildingType buildingType) {
        this.buildingType = buildingType;
    }

}
