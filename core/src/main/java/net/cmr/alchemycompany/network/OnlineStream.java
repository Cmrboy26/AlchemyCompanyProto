package net.cmr.alchemycompany.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.network.packet.EntityPacket;
import net.cmr.alchemycompany.network.packet.EntityPacket.EntityState;
import net.cmr.alchemycompany.network.packet.Packet;
import net.cmr.alchemycompany.network.packet.UUIDPacket;
import net.cmr.alchemycompany.network.packet.WorldPacket;

public class OnlineStream extends Stream {

    private Connection connection;
    private GameServer server;

    public OnlineStream(Connection connection, boolean isClient, GameServer server) {
        super(isClient);
        this.connection = connection;
        this.server = server;
        connection.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof Packet) {
                    synchronized (incomingPacketLock) {
                        incomingPackets.add((Packet) object);
                    }
                }
            }
        });
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
                EntityPacket entityPacket = new EntityPacket(entity, EntityState.ADDED);
                sendPacket(entityPacket);
            }
            WorldPacket packet = new WorldPacket(server.getWorld());
            sendPacket(packet);
        }
    }

    @Override
    protected void update() {
        // Since packets are automatically added to necessary lists when sent, do nothing.
        if (!connection.isConnected() && getState() != StreamState.DISCONNECTING && getState() != StreamState.FINISHED) {
            requestDisconnect();
        }
    }

    @Override
    protected void disconnect() throws IOException {
        connection.close();
        if (isClient()) {
            System.out.println("Client disconnect");
        } else {
            System.out.println("Server disconnect");
        }
    }

    protected GameServer getServer() {
        if (isClient() || server == null) {
            throw new RuntimeException("Server object is null.");
        }
        return server;
    }

    @Override
    public void sendPacket(Packet packet) {
        //System.out.println("Sending over tcp... "+packet.toString());
        connection.sendTCP(packet);
    }

    public static void registerKryo(Kryo kryo) {
        kryo.register(Serializable.class, new Serializer<Serializable>() {
            @Override
            public void write(Kryo kryo, Output output, Serializable object) {
                Json json = new Json();
                output.writeString(json.toJson(object, object.getClass()));
            }

            @Override
            public Serializable read(Kryo kryo, Input input, Class<? extends Serializable> type) {
                Json json = new Json();
                Serializable serializable = json.fromJson(type, input.readString());
                return serializable;
            }
        });
        kryo.register(UUID.class, new Serializer<UUID>() {

            @Override
            public void write(Kryo kryo, Output output, UUID object) {
                output.writeLong(object.getMostSignificantBits());
                output.writeLong(object.getLeastSignificantBits());
            }

            @Override
            public UUID read(Kryo kryo, Input input, Class<? extends UUID> type) {
                return new UUID(input.readLong(), input.readLong());
            }
            
        });
        kryo.setRegistrationRequired(false);
    }
}
