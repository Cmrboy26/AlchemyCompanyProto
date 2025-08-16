package net.cmr.alchemycompany.network.packet;

import net.cmr.alchemycompany.ecs.Entity;

public class EntityPacket extends Packet {

    public Entity entity;
    public boolean added;

    public EntityPacket() { }
    public EntityPacket(Entity entity, boolean added) {
        this.entity = entity.cloneEntity();
        this.added = added;
    }

    @Override
    public String toString() {
        return "EntityPacket{" +
            "entity=" + entity +
            ", added=" + added +
            '}';
    }

}
