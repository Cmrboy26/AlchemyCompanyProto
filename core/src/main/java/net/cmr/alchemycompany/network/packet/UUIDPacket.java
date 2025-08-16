package net.cmr.alchemycompany.network.packet;

import java.util.UUID;

public class UUIDPacket  extends Packet {

    public UUID id;

    public UUIDPacket() { }
    public UUIDPacket(UUID id) {
        this.id = id == null ? null : UUID.fromString(id.toString());
    }

}
