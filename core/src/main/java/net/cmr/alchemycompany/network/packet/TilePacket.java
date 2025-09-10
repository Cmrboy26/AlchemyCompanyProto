package net.cmr.alchemycompany.network.packet;

import net.cmr.alchemycompany.world.Tile;

public class TilePacket extends Packet {

    public Tile tile;

    public TilePacket() { }
    public TilePacket(Tile tile) {
        this.tile = tile;
    }
    
}
