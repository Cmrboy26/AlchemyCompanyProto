package net.cmr.alchemycompany.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import net.cmr.alchemycompany.network.packet.Packet;

public abstract class Stream {

    /*
     * Each gamemanager has an input and output Stream object, which is where information is sent and recieved between the server.
     * - LocalStream (same computer)
     * - OnlineStream (online)
     * Both stream objects have a "connect()", "waiting()", "join()", "update()" and "disconnect()" method.
     * - Connect initializes the connection (encryption, connecting online).
     * - Waiting is called repeatedly between connect and join if the player is in a lobby waiting to play.
     * - Join requests world and engine data from the server.
     * - Update checks for new packets from the server while the game is playing.
     * - Disconnect is when the player leaves.
     */

    protected Object outgoingPacketLock = new Object(), incomingPacketLock = new Object();
    protected Queue<Packet> incomingPackets;
    private StreamState state;
    private boolean isClient;

    public enum StreamState {
        CONNECTING,
        WAITING,
        JOINING,
        PLAYING,
        DISCONNECTING,
        FINISHED
    }

    public Stream(boolean isClient) {
        this.isClient = isClient;
        this.state = StreamState.CONNECTING;
        this.incomingPackets = new LinkedList<>();
    }

    /**
     * @throws IOException only if connection needs to be forcibly stopped.
     */
    public void updateStream() throws IOException {
        try {
            recievePackets();
            switch (state) {
                case CONNECTING:
                    connect(); // blocks
                    boolean isInLobby = false;
                    if (isInLobby) {
                        state = StreamState.WAITING;
                    } else {
                        state = StreamState.JOINING;
                    }
                    break;
                case WAITING:
                    // Get lobby updates
                    break;
                case JOINING:
                    join();
                    state = StreamState.PLAYING;
                    break;
                case PLAYING:
                    break;
                case DISCONNECTING:
                    disconnect();
                    state = StreamState.FINISHED;
                    break;
                case FINISHED:
                    // Do nothing.
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            // If there's an error in the connection, begin to disconnect from the game
            e.printStackTrace();
            if (state == StreamState.DISCONNECTING) {
                // There's an error with the disconnect method.
                throw new RuntimeException("Error with disconnect method.");
            }
            state = StreamState.DISCONNECTING;
        }
    }

    /**
     * Updates recieved packets.
     */
    protected abstract void recievePackets();
    /**
     * Initializes the connection (connects online, establishes encryption). Should block.
     */
    protected abstract void connect() throws IOException;
    /**
     * Checks for update packets (lobby-specific).
     */
    protected abstract void waiting() throws IOException;
    /**
     * Syncs the server state to the player. Should block.
     */
    protected abstract void join() throws IOException;
    /**
     * Sends notice to server that the client is disconnecting. Should block.
     */
    protected abstract void disconnect() throws IOException;

    public Packet pollIncomingPacket() {
        synchronized (incomingPacketLock) {
            return incomingPackets.poll();
        }
    }
    public List<Packet> pollAllPackets() {
        synchronized (incomingPacketLock) {
            List<Packet> list = new ArrayList<>(incomingPackets);
            incomingPackets.clear();
            return list;
        }
    }
    public List<Packet> peekAllPackets() {
        synchronized (incomingPacketLock) {
            List<Packet> list = new ArrayList<>(incomingPackets);
            return list;
        }
    }
    protected void waitUntilPacketRecieved(Class<? extends Packet> packetClass) {
        while (true) {
            recievePackets();
            if (peekAllPackets().stream().anyMatch(p -> {
                return packetClass.isInstance(p);
            })) {
                break;
            }
        }
    }

    /**
     * Sends packet immediately. Used for initializing.
     */
    public abstract void sendPacket(Packet packet);


    public boolean isClient() {
        return isClient;
    }
    public StreamState getState() {
        return state;
    }

}
