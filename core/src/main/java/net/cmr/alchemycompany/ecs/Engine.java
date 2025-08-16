package net.cmr.alchemycompany.ecs;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import net.cmr.alchemycompany.component.Component;

public abstract class Engine {

    private Map<UUID, Entity> entities;
    private Map<Class<? extends Component>, Set<Entity>> componentIndex;
    private Map<Class<? extends EntitySystem>, EntitySystem> systemMap;
    private Set<BiConsumer<Entity, Boolean>> entityChangeListeners;

    public Engine() {
        this.entities = new HashMap<>();
        this.componentIndex = new HashMap<>();
        this.systemMap = new HashMap<>();
        this.entityChangeListeners = new HashSet<>();
    }

    public void addEntity(Entity entity) {
        if (this.entities.containsKey(entity.getID())) {
            removeEntity(entities.get(entity.getID()));
        }
        this.entities.put(entity.getID(), entity);
        for (Class<? extends Component> componentClass : entity.getComponents().keySet()) {
            componentIndex.computeIfAbsent(componentClass, k -> new HashSet<>()).add(entity);
        }
        onEntityAdded(entity);
    }
    public void removeEntity(Entity entity) {
        this.entities.remove(entity.getID());
        for (Class<? extends Component> componentClass : entity.getComponents().keySet()) {
            componentIndex.computeIfAbsent(componentClass, k -> new HashSet<>()).remove(entity);
        }
        onEntityRemoved(entity);
    }
    public void changedEntity(Entity entity) {
        onEntityAdded(entity);
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
        system.engine = this;
        system.addedToEngine(this);
    }
    public void unregisterSystem(EntitySystem system) {
        systemMap.remove(system.getClass(), system);
        system.engine = null;
        system.removedFromEngine(this);
    }
    public Set<Entity> getEntities(Family family) {
        return entities.values().stream().filter(family::matches).collect(Collectors.toSet());
    }
    public Set<Entity> getEntities() {
        return Collections.unmodifiableSet(new HashSet<>(entities.values()));
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

    private Map<Class<? extends Component>, Set<Entity>> getComponentIndex() {
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
    protected void onEntityAdded(Entity entity) {
        for (BiConsumer<Entity, Boolean> listener : entityChangeListeners) {
            listener.accept(entity, true);
        }
    }
    protected void onEntityRemoved(Entity entity) {
        for (BiConsumer<Entity, Boolean> listener : entityChangeListeners) {
            listener.accept(entity, false);
        }
    }
    public void addEntityChangeListener(BiConsumer<Entity, Boolean> listener) {
        entityChangeListeners.add(listener);
    }
    public <T extends Engine> T as(Class<T> clazz) {
        return clazz.cast(this);
    }

}
