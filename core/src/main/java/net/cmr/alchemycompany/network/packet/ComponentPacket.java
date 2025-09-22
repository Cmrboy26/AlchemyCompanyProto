package net.cmr.alchemycompany.network.packet;

import net.cmr.alchemycompany.component.Component;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.network.packet.EntityPacket.EntityState;

public class ComponentPacket extends Packet {

    public String entityId;
    public String className;
    public Component component;

    public ComponentPacket() { }
    public ComponentPacket(Entity entity, Class<? extends Component> componentClazz, Component component) {
        this.entityId = entity.getID().toString();
        this.className = componentClazz.getName();
        this.component = component;
    }
    
}
