package net.cmr.alchemycompany.network.packet;

import net.cmr.alchemycompany.ecs.Entity;

public class EntityPacket extends Packet {

    public Entity entity;
    public EntityState entityState;
    
    public enum EntityState {
        ADDED,
        REMOVED
    }

    public EntityPacket() { }
    public EntityPacket(Entity entity, EntityState entityState) {
        this.entity = entity.cloneEntity();
        this.entityState = entityState;
    }

    @Override
    public String toString() {
        return "EntityPacket{" +
            "entity=" + entity.toShortString() +
            ", added=" + entityState +
            '}';
    }

}
