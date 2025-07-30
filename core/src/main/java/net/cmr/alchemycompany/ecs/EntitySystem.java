package net.cmr.alchemycompany.ecs;

public abstract class EntitySystem {
    
    protected Engine engine;

    public EntitySystem() {

    }
    public void addedToEngine(Engine engine) {
        this.engine = engine;
    }
    public void removedFromEngine(Engine engine) {
        
    }
    
}
