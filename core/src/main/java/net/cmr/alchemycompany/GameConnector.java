package net.cmr.alchemycompany;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.esotericsoftware.kryonet.Client;

import net.cmr.alchemycompany.component.actions.PlayerActionComponent;
import net.cmr.alchemycompany.component.actions.TurnActionComponent;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.network.GameServer;
import net.cmr.alchemycompany.network.LocalStream;
import net.cmr.alchemycompany.network.OnlineStream;
import net.cmr.alchemycompany.network.Stream;
import net.cmr.alchemycompany.network.Stream.StreamState;
import net.cmr.alchemycompany.network.packet.EntityPacket;
import net.cmr.alchemycompany.screen.GameScreen;

public class GameConnector {

    public static void hostGame() {
        GameServer server = new GameServer(true);
        Thread serverThread = new Thread(() -> {
            while (true) {
                server.update();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        LocalStream localStream = new LocalStream(server, true);
        server.addClientStream(localStream, false);
        startGameScreen(server, localStream);
    }

    public static void joinGame(String ip, int port) {
        Client client = new Client(8192, 8192);
        OnlineStream.registerKryo(client.getKryo());
        try {
            OnlineStream onlineStream = new OnlineStream(client, true, null);
            Thread clientUpdateThread = new Thread(() -> {
                boolean clientConnectedOnce = false;
                while (onlineStream.getState() != StreamState.FINISHED) {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } 

                    try {
                        client.update(50);
                        if (client.isConnected()) {
                            clientConnectedOnce = true;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        client.close();
                    }
                    try {
                        if (clientConnectedOnce || client.isConnected()) {
                            onlineStream.updateStream();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            clientUpdateThread.start();
            client.connect(ip, port);
            startGameScreen(null, onlineStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
        try {
            OnlineStream.registerKryo(client.getKryo());
            client.start();
            client.connect("127.0.0.1", 11265);
            OnlineStream os = new OnlineStream(client, turnFinished, server);
            Thread thread = new Thread(() -> {
                while (os.getState() != StreamState.PLAYING) {
                    try {
                        os.updateStream();
                        System.out.println("Updating stream, current state: " + os.getState());
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new RuntimeException("Forcibly disconnected.");
                    }
                }
                os.pollAllPackets();
                while (os.getState() != StreamState.FINISHED) {
                    try {
                        os.updateStream();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(1500);
                    } catch (Exception e) {
                    }
                    Entity entity = new Entity();
                    entity.addComponent(new PlayerActionComponent(playerUUID), null);
                    entity.addComponent(new TurnActionComponent(true), null);
                    os.sendPacket(new EntityPacket(entity, true));
                    System.out.println(os.getState());
                }
            });
            thread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
    }

    private static void startGameScreen(GameServer server, Stream stream) {
        GameScreen screen = new GameScreen(server, stream);
        AlchemyCompany.getInstance().setScreen(screen);
    }

}
