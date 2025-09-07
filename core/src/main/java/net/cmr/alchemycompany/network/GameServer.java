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

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import net.cmr.alchemycompany.ACEngine;
import net.cmr.alchemycompany.GameManager;
import net.cmr.alchemycompany.component.Component;
import net.cmr.alchemycompany.component.FogOfWarComponent;
import net.cmr.alchemycompany.component.OwnerComponent;
import net.cmr.alchemycompany.component.ResearchManagementComponent;
import net.cmr.alchemycompany.component.actions.IActionComponent;
import net.cmr.alchemycompany.component.actions.PlayerActionComponent;
import net.cmr.alchemycompany.component.actions.TurnActionComponent;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.network.Stream.StreamState;
import net.cmr.alchemycompany.network.packet.EntityPacket;
import net.cmr.alchemycompany.network.packet.Packet;
import net.cmr.alchemycompany.network.packet.TurnStatePacket.TurnState;
import net.cmr.alchemycompany.network.packet.UUIDPacket;
import net.cmr.alchemycompany.system.TurnSystem;
import net.cmr.alchemycompany.system.VisibilitySystem;
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
    private Server networkServer;
    private boolean allowConnectionMidGame;
    private boolean inLobby;
    private Map<UUID, List<Entity>> queuedActions;

    public GameServer(boolean allowConnectionMidGame) {
        this.allowConnectionMidGame = allowConnectionMidGame;
        this.inLobby = false;
        playerStreams = new HashMap<>();
        queuedBroadcasts = new ArrayList<>();
        playerStateListeners = new HashSet<>();
        computerPlayerStreams = new HashMap<>();
        queuedActions = new HashMap<>();

        World world = new World(WorldType.MINI, System.currentTimeMillis());
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
        try {
            initializeNetworkServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeNetworkServer() throws IOException {
        networkServer = new Server(16384, 16384);
        OnlineStream.registerKryo(networkServer.getKryo());
        networkServer.bind(11265);
        networkServer.start();
        networkServer.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                OnlineStream os = new OnlineStream(connection, false, GameServer.this);
                UUID playerUUID = initializeNewPlayer(os, false);
                connection.setArbitraryData(playerUUID);
            }
            @Override
            public void disconnected(Connection connection) {
                //System.out.println("DISCONNECTED");
                UUID playerUUID = (UUID) connection.getArbitraryData();
                onlinePlayerDisconnected(playerUUID);
            }
        });
        System.out.println("Server STARTED!");
    }

    private volatile long lastUpdate = System.nanoTime();

    public void update() {
        // Look for new streams
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
                        Thread.dumpStack();
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

        TurnState turnState = engine.getSystem(TurnSystem.class).getTurnState();
        for (UUID playerActionUUID : queuedActions.keySet()) {
            boolean isComputer = computerPlayerStreams.containsKey(playerActionUUID);
            if ((isComputer && turnState == TurnState.AI_TURN) || (!isComputer && turnState == TurnState.PLAYER_TURN)) {
                // Extra safeguard to make sure player doesn't queue multiple "ready" turn actions
                Entity turnActionEntity = null;
                for (Entity actionEntity : queuedActions.get(playerActionUUID)) {
                    if (actionEntity.hasComponent(TurnActionComponent.class)) {
                        TurnActionComponent tac = actionEntity.getComponent(TurnActionComponent.class);
                        if (tac.turnFinished) {
                            turnActionEntity = actionEntity;
                            break;
                        }
                    }
                }
                queuedActions.get(playerActionUUID).removeIf((e) -> { return e.hasComponent(TurnActionComponent.class); } );
                queuedActions.get(playerActionUUID).add(turnActionEntity);

                for (Entity actionEntity : queuedActions.get(playerActionUUID)) {
                    if (actionEntity != null) {
                        engine.addEntity(actionEntity);
                    }
                }
                queuedActions.remove(playerActionUUID);
            }
        }

        long now = System.nanoTime();
        float delta = (now - lastUpdate) / 1_000_000_000f;
        lastUpdate = now;
        engine.update(delta);
    }

    public void stop() {
        networkServer.stop();
    }

    public void dispose() {
        try {
            networkServer.dispose();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addClientStream(LocalStream clientStream, boolean isComputerPlayer) {
        synchronized (streamLock) {
            LocalStream serverStream = new LocalStream(this, false, clientStream);
            clientStream.otherStream = serverStream;
            initializeNewPlayer(serverStream, isComputerPlayer);
        }
    }

    public void onlinePlayerDisconnected(UUID playerUUID) {
        Stream stream = null;
        synchronized (streamLock) {
            stream = playerStreams.get(playerUUID);
            if (stream != null) {
                onPlayerDisconnected(playerUUID, stream);
            }
        }
    }

    private UUID initializeNewPlayer(Stream serverStream, boolean isComputerPlayer) {
        UUID playerUUID = UUID.randomUUID();
        playerStreams.put(playerUUID, serverStream);
        if (isComputerPlayer && serverStream instanceof LocalStream) {
            computerPlayerStreams.put(playerUUID, (LocalStream) serverStream);
        }
        serverStream.sendPacket(new UUIDPacket(playerUUID));

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
        int iterations = 0;
        while (iterations < 1000) {
            int x = new Random().nextInt((int) (world.width));
            int y = new Random().nextInt((int) (world.height));
            boolean visibleAnywhere = engine.getSystem(VisibilitySystem.class).isVisibleAnywhere(getPlayerUUIDs(), x, y);
            if (visibleAnywhere && iterations < 50) {
                continue;
            }
            boolean result = gameManager.tryPlaceBuilding(playerUUID, "HEADQUARTERS", x, y, true);
            // focusOnTile(x, y);
            if (result) {
                GameManager.onBuildingChange(playerUUID, x, y, engine);
                break;
            }
        }

        onPlayerConnected(playerUUID, playerStreams.get(playerUUID));
        return playerUUID;
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
                queuedActions.putIfAbsent(playerID, new ArrayList<>());
                queuedActions.get(playerID).add(entity);
                //getEngine().addEntity(entity);
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
        stream.requestDisconnect();
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
