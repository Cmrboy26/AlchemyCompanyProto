package net.cmr.alchemycompany.ecs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

public abstract class NotifyEntitySystem<T> extends EntitySystem {
    
    protected Collection<Consumer<T>> listeners;

    public NotifyEntitySystem() {
        this.listeners = new ArrayList<>();
    }

    public void addListener(Consumer<T> listener) {
        this.listeners.add(listener);
    }

    public void removeListener(Consumer<T> listener) {
        this.listeners.remove(listener);
    }

    public void clearListeners() {
        this.listeners.clear();
    }

    public Collection<Consumer<T>> getListeners() {
        return this.listeners;
    }

    public void notifyListeners(T event) {
        for (Consumer<T> listener : listeners) {
            listener.accept(event);
        }
    }

}
