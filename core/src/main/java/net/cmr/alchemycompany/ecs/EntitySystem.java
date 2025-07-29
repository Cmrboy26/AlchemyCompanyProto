package net.cmr.alchemycompany.ecs;

public abstract class EntitySystem {
    
    public EntitySystem() {

    }
    public abstract void addedToEngine(Engine engine);
    public abstract void removedFromEngine(Engine engine);
    
}
