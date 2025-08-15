package net.cmr.alchemycompany.ecs;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import net.cmr.alchemycompany.component.Component;

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
    public Entity getEntity(UUID id) {
        return this.entities.get(id);
    }
    public Entity getSingletonEntity(Class<? extends Component> componentClass) {
        Iterator<Entity> iterator = componentIndex.get(componentClass).iterator();
        if (iterator.hasNext()) return iterator.next();
        return null;
    }
    public <T extends Component> T getSingletonComponent(Class<T> componentClass) {
        return getSingletonEntity(componentClass).getComponent(componentClass);
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
    public Set<Entity> getEntitiesAny(Family...families) {
        return entities.values().stream().filter((e) -> {
            for (Family family : families) {
                if (family.matches(e)) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toSet());
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
