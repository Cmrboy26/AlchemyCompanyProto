package net.cmr.alchemycompany.network.packet;

public class TurnStatePacket extends Packet {
    
    public enum TurnState {
        PLAYER_TURN,
        AI_TURN,
        WORLD_TURN,
    }

    private TurnState state;
    private int turn;

    public TurnStatePacket() {

    }
    public TurnStatePacket(TurnState state, int turn) {
        this.state = state;
        this.turn = turn;
    }

    public TurnState getState() {
        return state;
    }

    public int getTurn() {
        return turn;
    }

    @Override
    public String toString() {
        return "["+state+" : "+turn+"]";
    }

}
