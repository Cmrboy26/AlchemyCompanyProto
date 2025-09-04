package net.cmr.alchemycompany.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import net.cmr.alchemycompany.ACEngine;
import net.cmr.alchemycompany.GameManager;
import net.cmr.alchemycompany.component.Component;
import net.cmr.alchemycompany.component.FogOfWarComponent;
import net.cmr.alchemycompany.component.OwnerComponent;
import net.cmr.alchemycompany.component.ResearchManagementComponent;
import net.cmr.alchemycompany.component.actions.IActionComponent;
import net.cmr.alchemycompany.component.actions.PlayerActionComponent;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.network.Stream.StreamState;
import net.cmr.alchemycompany.network.packet.EntityPacket;
import net.cmr.alchemycompany.network.packet.Packet;
import net.cmr.alchemycompany.network.packet.UUIDPacket;
import net.cmr.alchemycompany.world.World;
import net.cmr.alchemycompany.world.World.WorldType;

public class GameServer implements PlayerStateListener {

    private Object streamLock = new Object(), broadcastLock = new Object();
    private Map<UUID, Stream> playerStreams; // contains all player streams, including computer players
    private Map<UUID, LocalStream> computerPlayerStreams; // contains only computer player streams. localstream because all computer players are local.
    private ACEngine engine;
    private List<Packet> queuedBroadcasts;
    private GameManager gameManager;
    private Set<PlayerStateListener> playerStateListeners;

    public GameServer() {
        playerStreams = new HashMap<>();
        queuedBroadcasts = new ArrayList<>();
        playerStateListeners = new HashSet<>();
        computerPlayerStreams = new HashMap<>();

        World world = new World(WorldType.SMALL, System.currentTimeMillis());
        ACEngine engine = GameManager.createServerEngine(this, world);
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

    public void addClientStream(LocalStream clientStream, boolean isComputerPlayer) {
        synchronized (streamLock) {
            LocalStream serverStream = new LocalStream(this, false, clientStream);
            clientStream.otherStream = serverStream;
            serverStream.setServerObject(this);
            UUID playerUUID = UUID.randomUUID();
            playerStreams.put(playerUUID, serverStream);
            if (isComputerPlayer) {
                computerPlayerStreams.put(playerUUID, serverStream);
            }
            serverStream.sendPacket(new UUIDPacket(playerUUID));
            initializeNewPlayer(playerUUID);
        }
    }

    private void initializeNewPlayer(UUID playerUUID) {
        // Add any initialization logic for a new player here.

        // TODO: maybe make a new thread and block until the player has sent their ready signal?
        Entity fogEntity = new Entity();
        fogEntity.addComponent(new OwnerComponent(playerUUID), engine);
        fogEntity.addComponent(new FogOfWarComponent(), engine);
        engine.addEntity(fogEntity);

        Entity researchEntity = new Entity();
        researchEntity.addComponent(new ResearchManagementComponent(), engine);
        researchEntity.addComponent(new OwnerComponent(playerUUID), engine);
        engine.addEntity(researchEntity);

        World world = engine.getWorld();
        while (true) {
            int x = new Random().nextInt((int) (world.width));
            int y = new Random().nextInt((int) (world.height));
            boolean result = gameManager.tryPlaceBuilding(playerUUID, "HEADQUARTERS", x, y, true);
            // focusOnTile(x, y);
            if (result) {
                GameManager.onBuildingChange(playerUUID, x, y, engine);
                break;
            }
        }

        onPlayerConnected(playerUUID, playerStreams.get(playerUUID));
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
    public Set<UUID> getPlayerUUIDs() {
        return playerStreams.keySet();
    }
    public Map<UUID, Stream> getPlayerStreams() {
        return Collections.unmodifiableMap(playerStreams);
    }
    public Map<UUID, Stream> getComputerPlayerStreams() {
        return Collections.unmodifiableMap(computerPlayerStreams);
    }

    public void registerPlayerStateListener(PlayerStateListener listener) {
        playerStateListeners.add(listener);
    }
    public void unregisterPlayerStateListener(PlayerStateListener listener) {
        playerStateListeners.remove(listener);
    }

    @Override
    public void onPlayerConnected(UUID playerUUID, Stream stream) {
        // TODO: add multithreading support for connected and disconnected
        Set<PlayerStateListener> finishedListeners = new HashSet<>();
        for (PlayerStateListener listener : playerStateListeners) {
            listener.onPlayerConnected(playerUUID, stream);
            if (listener.isFinished()) {
                finishedListeners.add(listener);
            }
        }
        for (PlayerStateListener listener : finishedListeners) {
            playerStateListeners.remove(listener);
        }
    }

    @Override
    public void onPlayerDisconnected(UUID playerUUID, Stream stream) {
        // TODO: if a player disconnects, create a computer player to take over
        Set<PlayerStateListener> finishedListeners = new HashSet<>();
        for (PlayerStateListener listener : playerStateListeners) {
            listener.onPlayerDisconnected(playerUUID, stream);
            if (listener.isFinished()) {
                finishedListeners.add(listener);
            }
        }
        for (PlayerStateListener listener : finishedListeners) {
            playerStateListeners.remove(listener);
        }
    }

}
