package net.cmr.alchemycompany.ecs;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.badlogic.gdx.utils.Null;

public class Entity implements Serializable, Cloneable {
    
    private final Map<Class<? extends Component>, Component> componentMap;
    private final UUID id; 

    public Entity() {
        this.id = UUID.randomUUID();
        this.componentMap = new HashMap<>();
    }
    
    public Map<Class<? extends Component>, Component> getComponents() {
        return componentMap;
    }
    public void addComponent(Component component, @Null Engine engine) {
        if (componentMap.containsKey(component.getClass())) {
            throw new IllegalArgumentException("Component of type " + component.getClass().getSimpleName() + " already exists in this entity.");
        }
        componentMap.put(component.getClass(), component);
        if (engine != null) engine.onAddComponent(this, component);
    }
    public void removeComponent(Class<? extends Component> componentClass, @Null Engine engine) {
        Component component = componentMap.remove(componentClass);
        if (engine != null) engine.onRemoveComponent(this, component);
    }
    public boolean hasComponent(Class<? extends Component> componentClass) {
        return componentMap.containsKey(componentClass);
    }
    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(Class<T> clazz) {
        return (T) componentMap.get(clazz);
    }
    public UUID getID() {
        return id;
    }

    @Override
    public String toString() {
        return "Entity{" +
                "id=" + id +
                ", components=" + componentMap.values() +
                '}';
    }

}
