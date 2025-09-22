package net.cmr.alchemycompany.network.packet;

import net.cmr.alchemycompany.world.World;

public class WorldPacket extends Packet {

    public World world;

    public WorldPacket() { }
    public WorldPacket(World world) {
        this.world = (World) world.clone();
    }

}
