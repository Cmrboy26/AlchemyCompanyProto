package net.cmr.alchemycompany.ecs;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import net.cmr.alchemycompany.component.Component;
import net.cmr.alchemycompany.network.packet.EntityPacket.EntityState;

public abstract class Engine {

    private Map<UUID, Entity> entities;
    private Map<Class<? extends Component>, Set<Entity>> componentIndex;
    private Map<Class<? extends EntitySystem>, EntitySystem> systemMap;
    private Set<BiConsumer<Entity, EntityState>> entityChangeListeners;
    private Set<BiConsumer<Entity, Class<? extends Component>>> componentChangeListeners;

    public Engine() {
        this.entities = new ConcurrentHashMap<>();
        this.componentIndex = new ConcurrentHashMap<>();
        this.systemMap = new ConcurrentHashMap<>();
        this.entityChangeListeners = new HashSet<>();
        this.componentChangeListeners = new HashSet<>();
    }

    public void addEntity(Entity entity) {
        if (this.entities.containsKey(entity.getID())) {
            Entity at = entities.get(entity.getID());
            for (Class<? extends Component> componentClass : at.getComponents().keySet()) {
                componentIndex.computeIfAbsent(componentClass, k -> new HashSet<>());
                componentIndex.get(componentClass).remove(at);
            }
        }

        this.entities.put(entity.getID(), entity);
        for (Class<? extends Component> componentClass : entity.getComponents().keySet()) {
            componentIndex.computeIfAbsent(componentClass, k -> new HashSet<>());
            componentIndex.get(componentClass).add(entity);
        }
        onEntityAdded(entity);
    }
    public void removeEntity(Entity entity) {
        this.entities.remove(entity.getID());
        for (Class<? extends Component> componentClass : entity.getComponents().keySet()) {
            componentIndex.computeIfAbsent(componentClass, k -> new HashSet<>());
            componentIndex.get(componentClass).remove(entity);
        }
        onEntityRemoved(entity);
    }
    /*@Deprecated
    public void changedEntity(Entity entity) {
        onEntityAdded(entity);
    }*/
    public void changedComponent(Entity entity, Class<? extends Component> componentClass) {
        onComponentChanged(entity, componentClass);
    }
    /**
     * Finds the entity with the current ID.
     * @param id UUID of the entity
     * @return entity specified, otherwise null if not found
     */
    public Entity getEntity(UUID id) {
        if (id == null) {
            return null;
        }
        return this.entities.getOrDefault(id, null);
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
    public Map<Class<? extends EntitySystem>, EntitySystem> getSystems() {
        return Collections.unmodifiableMap(systemMap);
    }

    public Set<Entity> getComponentMapper(Class<? extends Component> componentClass) {
        return Collections.unmodifiableSet(componentIndex.computeIfAbsent(componentClass, k -> new HashSet<>()));
    }

    private Map<Class<? extends Component>, Set<Entity>> getComponentIndex() {
        return componentIndex;
    }

    protected void onRemoveComponent(Entity entity, Component component) {
        if (component == null) {
            return;
        }
        Set<Entity> indexed = getComponentIndex().get(component.getClass());
        if (indexed != null) {
            indexed.remove(entity);
        }
        // notify listeners
        onComponentChanged(entity, component.getClass());
    }
    protected void onAddComponent(Entity entity, Component component) {
        Set<Entity> indexed = getComponentIndex().get(component.getClass());
        if (indexed != null) {
            indexed.add(entity);
        }
        // notify listeners
        onComponentChanged(entity, component.getClass());
    }
    protected void onEntityAdded(Entity entity) {
        for (BiConsumer<Entity, EntityState> listener : entityChangeListeners) {
            listener.accept(entity, EntityState.ADDED);
        }
    }
    protected void onEntityRemoved(Entity entity) {
        for (BiConsumer<Entity, EntityState> listener : entityChangeListeners) {
            listener.accept(entity, EntityState.REMOVED);
        }
    }
    protected void onComponentChanged(Entity entity, Class<? extends Component> componentClass) {
        for (BiConsumer<Entity, Class<? extends Component>> listener : componentChangeListeners) {
            listener.accept(entity, componentClass);
        }
    }
    public void addEntityChangeListener(BiConsumer<Entity, EntityState> listener) {
        entityChangeListeners.add(listener);
    }
    public void addComponentChangeListener(BiConsumer<Entity, Class<? extends Component>> listener) {
        componentChangeListeners.add(listener);
    }
    public <T extends Engine> T as(Class<T> clazz) {
        return clazz.cast(this);
    }

}
