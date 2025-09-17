package net.cmr.alchemycompany.ecs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.Null;

import net.cmr.alchemycompany.component.Component;

public class Entity implements Serializable, Cloneable {

    private HashMap<Class<? extends Component>, Component> componentMap;
    private UUID id;

    public Entity() {
        this.id = UUID.randomUUID();
        this.componentMap = new HashMap<>();
    }

    public Map<Class<? extends Component>, Component> getComponents() {
        return componentMap;
    }
    /**
     * Adds a component if the component doesn't exist.
     * @throws IllegalArgumentException if component already exists in the entity.
     */
    public void addComponent(Component component, @Null Engine engine) {
        if (componentMap.containsKey(component.getClass())) {
            throw new IllegalArgumentException("Component of type " + component.getClass().getSimpleName() + " already exists in this entity.");
        }
        setComponent(component, engine);
    }
    /**
     * Stronger version of addComponent. Does not check for existing components and overrides anything at the current location
     */
    public void setComponent(Component component, @Null Engine engine) {
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
    /**
     * @return desired component if entity has it, otherwise returns null.
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(Class<T> clazz) {
        if (!componentMap.containsKey(clazz)) {
            return null;
        }
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

    public String toShortString() {
        return toString().substring(0, Math.min(200, toString().length()))+"...";
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        id = UUID.fromString(jsonData.getString("id", UUID.randomUUID().toString()));
        componentMap = new HashMap<>();
        ArrayList<Component> componentSet = json.readValue("components", ArrayList.class, jsonData);
        for (Component component : componentSet) {
            componentMap.put(component.getClass(), component);
        }
    }

    @Override
    public void write(Json json) {
        json.writeValue("id", id.toString());
        ArrayList<Component> componentSet = new ArrayList<>(componentMap.values());
        json.writeValue("components", componentSet);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        Json json = new Json();
        String jsonString = json.toJson(this);
        return json.fromJson(Entity.class, jsonString);
    }

    public Entity cloneEntity() {
        return cloneEntity(false);
    }
    public Entity cloneEntity(boolean newUUID) {
        try {
            Entity entity = (Entity) clone();
            if (newUUID) {
                entity.id = UUID.randomUUID();
            }
            return entity;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("Cloning not supported", e);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Entity)) {
            return false;
        }
        Entity entity = (Entity) obj;
        if (entity.getID().equals(this.getID())) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getID().hashCode();
    }

}
