package net.cmr.alchemycompany.ecs;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class Engine {
    
    private Map<UUID, Entity> entities;
    private Map<Class<? extends Component>, Set<Entity>> componentIndex;
    private Map<Class<? extends EntitySystem>, EntitySystem> systemMap;

    public Engine() {
        this.entities = new HashMap<>();
        this.componentIndex = new HashMap<>();
        this.systemMap = new HashMap<>();
    }

    public void addEntity(Entity entity) {
        this.entities.put(entity.getID(), entity);
        for (Class<? extends Component> componentClass : entity.getComponents().keySet()) {
            componentIndex.computeIfAbsent(componentClass, k -> new HashSet<>()).add(entity);
        }
    }
    public void removeEntity(Entity entity) {
        this.entities.remove(entity.getID());
        for (Class<? extends Component> componentClass : entity.getComponents().keySet()) {
            componentIndex.computeIfAbsent(componentClass, k -> new HashSet<>()).remove(entity);
        }
    }
    public void registerSystem(EntitySystem system) {
        systemMap.put(system.getClass(), system);
        system.addedToEngine(this);
    }
    public void unregisterSystem(EntitySystem system) {
        systemMap.remove(system.getClass(), system);
        system.removedFromEngine(this);
    }
    public Set<Entity> getEntities(Family family) {
        return entities.values().stream().filter(family::matches).collect(Collectors.toSet());
    }
    @SuppressWarnings("unchecked")
    public <T extends EntitySystem> T getSystem(Class<T> systemClass) {
        return (T) systemMap.get(systemClass);
    }

    public Set<Entity> getComponentMapper(Class<? extends Component> componentClass) {
        return Collections.unmodifiableSet(componentIndex.computeIfAbsent(componentClass, k -> new HashSet<>()));
    }

    private  Map<Class<? extends Component>, Set<Entity>> getComponentIndex() {
        return componentIndex;
    }

    protected void onRemoveComponent(Entity entity, Component component) {
        Set<Entity> indexed = getComponentIndex().get(component.getClass());
        if (indexed != null) {
            indexed.remove(entity);
        }
        // notify listeners 
    }
    protected void onAddComponent(Entity entity, Component component) {
        Set<Entity> indexed = getComponentIndex().get(component.getClass());
        if (indexed != null) {
            indexed.add(entity);
        }
        // notify listeners
    }

}
