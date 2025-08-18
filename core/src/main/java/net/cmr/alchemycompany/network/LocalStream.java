package net.cmr.alchemycompany.network;

import java.io.IOException;

import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.network.packet.EntityPacket;
import net.cmr.alchemycompany.network.packet.Packet;
import net.cmr.alchemycompany.network.packet.UUIDPacket;
import net.cmr.alchemycompany.network.packet.WorldPacket;

public class LocalStream extends Stream {

    protected LocalStream otherStream;
    protected GameServer server;

    public LocalStream(GameServer server, boolean isClient) {
        super(isClient);
        this.server = server;
    }
    public LocalStream(GameServer server, boolean isClient, LocalStream otherStream) {
        super(isClient);
        this.server = server;
        this.otherStream = otherStream;
    }

    @Override
    protected void connect() throws IOException {
        if (isClient()) {
            waitUntilPacketRecieved(UUIDPacket.class);
        } else {

        }
    }

    @Override
    protected void waiting() throws IOException {
        if (isClient()) {
            System.out.println("Client wait.");
        } else {
            System.out.println("Server wait.");
        }
    }

    @Override
    protected void join() throws IOException {
        if (isClient()) {
            waitUntilPacketRecieved(WorldPacket.class);
        } else {
            for (Entity entity : getServer().getEngine().getEntities()) {
                EntityPacket entityPacket = new EntityPacket(entity, true);
                sendPacket(entityPacket);
            }
            WorldPacket packet = new WorldPacket(server.getWorld());
            sendPacket(packet);
        }
    }

    @Override
    protected void recievePackets() {
        // Since packets are automatically added to necessary lists when sent, do nothing.
    }

    @Override
    protected void disconnect() throws IOException {
        if (isClient()) {
            System.out.println("Client disconnect");
        } else {
            System.out.println("Server disconnect");
        }
    }
    @Override
    public void sendPacket(Packet packet) {
        synchronized (otherStream.incomingPacketLock) {
            otherStream.incomingPackets.add(packet);
        }
    }

    public void setServerObject(GameServer server) {
        this.server = server;
    }

    protected GameServer getServer() {
        if (isClient() || server == null) {
            throw new RuntimeException("Server object is null.");
        }
        return server;
    }

}
