package net.cmr.alchemycompany.ecs;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.cmr.alchemycompany.component.Component;

public class Family {
    
    private final Set<Class<? extends Component>> familyComponents;

    private Family(Set<Class<? extends Component>> familyComponents) {
        this.familyComponents = familyComponents;
    }

    public boolean matches(Entity entity) {
        for (Class<? extends Component> cls : familyComponents) {
            if (!entity.hasComponent(cls)) return false;
        }
        return true;
    }

    @SafeVarargs
    public static Family all(Class<? extends Component>... familyComponents) {
        return new Family(new HashSet<>(Arrays.asList(familyComponents)));
    }

}
