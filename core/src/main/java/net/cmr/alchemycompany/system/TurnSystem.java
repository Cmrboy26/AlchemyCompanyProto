package net.cmr.alchemycompany.system;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import net.cmr.alchemycompany.ITurnSystem;
import net.cmr.alchemycompany.IUpdateSystem;
import net.cmr.alchemycompany.component.actions.IActionComponent;
import net.cmr.alchemycompany.component.actions.PlayerActionComponent;
import net.cmr.alchemycompany.component.actions.TurnActionComponent;
import net.cmr.alchemycompany.ecs.Engine;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.ecs.EntitySystem;
import net.cmr.alchemycompany.network.GameServer;
import net.cmr.alchemycompany.network.PlayerStateListener;
import net.cmr.alchemycompany.network.Stream;
import net.cmr.alchemycompany.network.packet.EntityPacket;
import net.cmr.alchemycompany.network.packet.TurnStatePacket;
import net.cmr.alchemycompany.network.packet.EntityPacket.EntityState;
import net.cmr.alchemycompany.network.packet.TurnStatePacket.TurnState;

public class TurnSystem extends EntitySystem implements IUpdateSystem, PlayerStateListener {
    
    private GameServer server;
    // I know this is bad practice for ECS, sorry:
    private Map<UUID, Boolean> playerTurnStates, aiTurnStates;
    private TurnState turnState;
    private int turn = 1;

    public TurnSystem(GameServer server) {
        this.server = server;
        this.playerTurnStates = new HashMap<>();
        this.aiTurnStates = new HashMap<>();
        this.server.registerPlayerStateListener(this);
        this.turnState = TurnState.PLAYER_TURN;
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
    }
    
    public static void broadcastTurnState(boolean turnFinished, UUID currentPlayer, Stream stream) {
        Entity entity = new Entity();
        entity.addComponent(new TurnActionComponent(turnFinished), null);
        entity.addComponent(new PlayerActionComponent(currentPlayer), null);
        stream.sendPacket(new EntityPacket(entity, EntityState.ADDED));
    }

    private void postProcessTurn() {
        // If all players have finished their turn, start a new turn.

        switch (turnState) {
            case PLAYER_TURN:
                boolean allFinished = true;
                for (boolean finished : playerTurnStates.values()) {
                    if (!finished) {
                        allFinished = false;
                        break;
                    }
                }
                if (allFinished) {
                    turnState = TurnState.AI_TURN;
                    turn++;

                    // Reset all players to not finished.
                    for (UUID playerUUID : playerTurnStates.keySet()) {
                        playerTurnStates.put(playerUUID, false);
                        TurnStatePacket tsp = new TurnStatePacket(turnState, turn);
                        server.getPlayerStreams().get(playerUUID).sendPacket(tsp);
                    }
                    // Tell AI to make their moves.
                    for (UUID playerUUID : aiTurnStates.keySet()) {
                        TurnStatePacket tsp = new TurnStatePacket(turnState, turn);
                        server.getComputerPlayerStreams().get(playerUUID).sendPacket(tsp);
                    }

                    List<ITurnSystem> turnSystems = engine.getSystems().values().stream()
                        .filter(s -> s instanceof ITurnSystem)
                        .map(s -> (ITurnSystem) s)
                        .collect(Collectors.toList());
                    Collections.sort(turnSystems, (s1, s2) -> {
                        return Integer.compare(s1.getTurnPriority(), s2.getTurnPriority());
                    });

                    for (ITurnSystem turnSystem : turnSystems) {
                        // System.out.println("TurnSystem processing: "+turnSystem.getClass().getSimpleName());
                        turnSystem.onTurn();
                    }
                    postProcessTurn();
                }
                break;
            case AI_TURN:

                try {
                    Thread.sleep(250);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                boolean aiFinished = true;
                for (boolean finished : aiTurnStates.values()) {
                    if (!finished) {
                        allFinished = false;
                        break;
                    }
                }
                if (aiFinished) {
                    turnState = TurnState.PLAYER_TURN;
                    // Reset all players to not finished.
                    // Reset all players to not finished.
                    for (UUID playerUUID : playerTurnStates.keySet()) {
                        playerTurnStates.put(playerUUID, false);
                        TurnStatePacket tsp = new TurnStatePacket(turnState, turn);
                        server.getPlayerStreams().get(playerUUID).sendPacket(tsp);
                    }
                    // Tell AI to make their moves.
                    for (UUID playerUUID : aiTurnStates.keySet()) {
                        TurnStatePacket tsp = new TurnStatePacket(turnState, turn);
                        server.getComputerPlayerStreams().get(playerUUID).sendPacket(tsp);
                    }
                }
                break;
            case WORLD_TURN:
                
                break;
        }
    }

    @Override
    public void update(float delta) {
        IActionComponent.processActionEntities(engine, TurnActionComponent.class, (entity) -> {
            PlayerActionComponent pac = entity.getComponent(PlayerActionComponent.class);
            TurnActionComponent tac = entity.getComponent(TurnActionComponent.class);

            UUID playerID = pac.playerUUID;
            boolean turnFinished = tac.turnFinished;

            System.out.println("TurnSystem received action "+pac+"\n"+tac);

            setPlayerTurnState(playerID, turnFinished);
        });
    }

    public TurnState getTurnState() {
        return turnState;
    }

    private void setPlayerTurnState(UUID playerUUID, boolean turnFinished) {
        if (!server.getComputerPlayerStreams().containsKey(playerUUID)) {
            playerTurnStates.put(playerUUID, turnFinished);
            postProcessTurn();
        } else {
            aiTurnStates.put(playerUUID, turnFinished);
            postProcessTurn();
        }
    }

    private void removePlayer(UUID playerUUID) {
        if (!server.getComputerPlayerStreams().containsKey(playerUUID)) {
            playerTurnStates.remove(playerUUID);
            postProcessTurn();
        } else {
            aiTurnStates.remove(playerUUID);
            postProcessTurn();
        }
    }

    @Override
    public void onPlayerConnected(UUID playerUUID, Stream stream) {
        setPlayerTurnState(playerUUID, false);
        //System.out.println("TurnSystem: Player connected: "+playerUUID);
        stream.sendPacket(new TurnStatePacket(turnState, turn));
    }

    @Override
    public void onPlayerDisconnected(UUID playerUUID, Stream stream) {
        removePlayer(playerUUID);
    }

}
