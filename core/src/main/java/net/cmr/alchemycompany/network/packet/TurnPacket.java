package net.cmr.alchemycompany.network.packet;

public class TurnPacket extends Packet {
    
    public enum TurnState {
        PLAYER_TURN,
        AI_TURN,
        WORLD_TURN,
    }

    private TurnState state;

    public TurnPacket() {

    }
    public TurnPacket(TurnState state) {
        this.state = state;
    }

    public TurnState getState() {
        return state;
    }

}
