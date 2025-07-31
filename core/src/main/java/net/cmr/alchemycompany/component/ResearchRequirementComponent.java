package net.cmr.alchemycompany.component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.cmr.alchemycompany.ecs.Component;
import net.cmr.alchemycompany.game.Technologies.Technology;

public class ResearchRequirementComponent extends Component {
    
    public Set<Technology> technologiesRequired;

    public ResearchRequirementComponent(Set<Technology> technologiesRequired) {
        this.technologiesRequired = technologiesRequired;
    }

    public ResearchRequirementComponent(Technology technologyRequired) {
        Set<Technology> techSet = new HashSet<>();
        techSet.add(technologyRequired);
        this.technologiesRequired = techSet;
    }

}
