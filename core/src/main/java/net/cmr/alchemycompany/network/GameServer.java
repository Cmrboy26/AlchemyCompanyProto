package net.cmr.alchemycompany.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.badlogic.gdx.utils.Array;

import net.cmr.alchemycompany.ACEngine;
import net.cmr.alchemycompany.GameManager;
import net.cmr.alchemycompany.component.Component;
import net.cmr.alchemycompany.component.actions.IActionComponent;
import net.cmr.alchemycompany.component.actions.PlayerActionComponent;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.entity.BuildingFactory.BuildingType;
import net.cmr.alchemycompany.network.Stream.StreamState;
import net.cmr.alchemycompany.network.packet.EntityPacket;
import net.cmr.alchemycompany.network.packet.Packet;
import net.cmr.alchemycompany.network.packet.UUIDPacket;
import net.cmr.alchemycompany.system.BuildingManagementSystem;
import net.cmr.alchemycompany.world.World;
import net.cmr.alchemycompany.world.World.WorldType;

public class GameServer {

    private Object streamLock = new Object(), broadcastLock = new Object();
    private Map<UUID, Stream> playerStreams; // player is assigned randomly selected uuid
    private ACEngine engine;
    private List<Packet> queuedBroadcasts;
    private GameManager gameManager;

    public GameServer() {
        playerStreams = new HashMap<>();
        queuedBroadcasts = new ArrayList<>();

        World world = new World(WorldType.SMALL, System.currentTimeMillis());
        ACEngine engine = GameManager.createServerEngine(world);
        this.engine = engine;
        this.engine.setWorld(world);
        engine.addEntityChangeListener((entity, added) -> {
            if (entity.hasComponent(PlayerActionComponent.class)) {
                // Don't broadcast player actions.
                return;
            }
            EntityPacket packet = new EntityPacket(entity, added);
            synchronized (broadcastLock) {
                queuedBroadcasts.add(packet);
            }
        });
        gameManager = new GameManager(null, engine, getWorld());
    }

    private volatile long lastUpdate = System.nanoTime();

    public void update() {
        // Look for new streams
        searchForOnlineStreams();
        List<Packet> packetQueue = new ArrayList<>();
        synchronized (broadcastLock) {
            packetQueue.addAll(queuedBroadcasts);
            queuedBroadcasts.clear();
        }
        synchronized (streamLock) {
            for (Packet packet : packetQueue) {
                for (UUID playerID : playerStreams.keySet()) {
                    Stream stream = playerStreams.get(playerID);
                    stream.sendPacket(packet);
                    //System.out.println("[DEBUG] Sent packet "+packet);
                }
            }

            List<UUID> removeStreams = new ArrayList<>();
            for (UUID playerID : playerStreams.keySet()) {
                Stream stream = playerStreams.get(playerID);
                try {
                    StreamState previousState = stream.getState();
                    stream.updateStream();
                    if (stream.getState() == StreamState.FINISHED) {
                        removeStreams.add(playerID);
                        // Update players about stream removal
                        continue;
                    }
                    if (previousState == StreamState.CONNECTING && stream.getState() != StreamState.CONNECTING) {
                        // Player has successfully connected.
                    }
                    if (stream.getState() == StreamState.PLAYING) {
                        List<Packet> newPackets = stream.pollAllPackets();
                        for (Packet packet : newPackets) {
                            processPacket(playerID, stream, packet);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    removeStreams.add(playerID);
                }
            }
            while (!removeStreams.isEmpty()) {
                playerStreams.remove(removeStreams.get(0));
                removeStreams.remove(0);
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        long now = System.nanoTime();
        float delta = (now - lastUpdate) / 1_000_000_000f;
        lastUpdate = now;
        engine.update(delta);
    }

    private void searchForOnlineStreams() {
        // Check network for new connections.
        // If new connection, create a stream.
    }

    public void addClientStream(LocalStream clientStream) {
        synchronized (streamLock) {
            LocalStream serverStream = new LocalStream(this, false, clientStream);
            clientStream.otherStream = serverStream;
            serverStream.setServerObject(this);
            UUID playerUUID = UUID.randomUUID();
            playerStreams.put(playerUUID, serverStream);
            serverStream.sendPacket(new UUIDPacket(playerUUID));

            World world = engine.getWorld();
            while (true) {
                int x = new Random().nextInt((int) (world.width / 10f));
                int y = new Random().nextInt((int) (world.height / 10f));
                boolean result = gameManager.tryPlaceBuilding(playerUUID, BuildingType.HEADQUARTERS, x, y, true);
                //focusOnTile(x, y);
                if (result) {
                    GameManager.onBuildingChange(playerUUID, x, y, engine);
                    break;
                }
            }
        }
    }

    public void processPacket(UUID playerID, Stream stream, Packet packet) {
        System.out.println("Server processing packet: "+packet.toString());
        if (packet instanceof EntityPacket) {
            EntityPacket entityPacket = (EntityPacket) packet;
            Entity entity = entityPacket.entity;
            if (entity != null && entity.hasComponent(PlayerActionComponent.class)) {
                PlayerActionComponent pac = entity.getComponent(PlayerActionComponent.class);
                // Ensure the action is being performed by the same player
                pac.playerUUID = playerID;
                for (Component component : entity.getComponents().values()) {
                    if (!(component instanceof IActionComponent)) {
                        // Entity has a non-action component. Must be disgarded
                        System.out.println("WARNING: Packet contains a non-action component. Developer error or cheating detected.");
                        return;
                    }
                }
                getEngine().addEntity(entity);
            } else {
                // DO NOTHING: they sent over a non-action component.
            }
        }
    }

    protected void broadcastPacket(Packet packet) {
        synchronized (broadcastLock) {
            queuedBroadcasts.add(packet);
        }
    }

    public ACEngine getEngine() {
        return engine;
    }
    public World getWorld() {
        return engine.getWorld();
    }

}
