package net.cmr.alchemycompany.screen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.esotericsoftware.kryonet.Client;

import net.cmr.alchemycompany.ACEngine;
import net.cmr.alchemycompany.AlchemyCompany;
import net.cmr.alchemycompany.GameManager;
import net.cmr.alchemycompany.IsometricHelper;
import net.cmr.alchemycompany.Sprites;
import net.cmr.alchemycompany.component.BuildingComponent;
import net.cmr.alchemycompany.component.OwnerComponent;
import net.cmr.alchemycompany.component.TilePositionComponent;
import net.cmr.alchemycompany.component.actions.PlayerActionComponent;
import net.cmr.alchemycompany.component.actions.TurnActionComponent;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.helper.InputHelper;
import net.cmr.alchemycompany.helper.MenuHelper;
import net.cmr.alchemycompany.network.GameServer;
import net.cmr.alchemycompany.network.LocalStream;
import net.cmr.alchemycompany.network.OnlineStream;
import net.cmr.alchemycompany.network.Stream;
import net.cmr.alchemycompany.network.Stream.StreamState;
import net.cmr.alchemycompany.network.packet.EntityPacket;
import net.cmr.alchemycompany.network.packet.Packet;
import net.cmr.alchemycompany.network.packet.UUIDPacket;
import net.cmr.alchemycompany.network.packet.WorldPacket;
import net.cmr.alchemycompany.system.RenderSystem;
import net.cmr.alchemycompany.system.ResourceSystem;
import net.cmr.alchemycompany.world.World;

public class GameScreen implements Screen {

    private Viewport uiViewport, worldViewport;
    private Stage stage;
    private Skin skin;
    private int lastMouseX, lastMouseY;
    private UUID playerUUID;
    private int focusX = -1, focusY = -1;
    public MenuHelper menuHelper;
    public InputHelper inputHelper;

    private GameManager gameManager;
    private Stream stream;
    private GameServer server;

    public GameScreen(GameServer server, Stream stream) {
        this.server = server;
        this.stream = stream;
    }

    @Override
    public void show() {
        prepareGame();
        prepareUI();
        setupInputProcessor();
    }

    @Override
    public void render(float delta) {
        // Camera drag with right mouse button
        checkConnection();
        processTileFocus();
        updateInput();
        gameManager.getEngine().update(delta);
        List<Packet> polledPackets = stream.pollAllPackets();
        for (Packet packet : polledPackets) {
            if (packet instanceof EntityPacket) {
                EntityPacket ep = (EntityPacket) packet;
                Entity entity = ep.entity;
                boolean added = ep.added;
                //System.out.println("[DEBUG] Client recieved "+entity.toShortString());
                if (added) {
                    gameManager.getEngine().addEntity(entity);
                } else {
                    gameManager.getEngine().removeEntity(entity);
                }
            }
        }

        SpriteBatch batch = AlchemyCompany.getInstance().batch();
        worldViewport.apply();
        batch.setProjectionMatrix(worldViewport.getCamera().combined);
        batch.setColor(Color.WHITE);
        gameManager.getEngine().render(playerUUID, batch, delta);

        batch.setProjectionMatrix(uiViewport.getCamera().combined);
        uiViewport.apply();
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        worldViewport.update(width, height, true);
        uiViewport.update(width, height, true);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {
        if (server != null) {
            server.stop();
            server.dispose();
        }
    }

    @Override
    public void dispose() {
        stage.dispose();
        if (server != null) {
            server.stop();
            server.dispose();
        }
    }

    private void prepareGame() {
        // Connected. Process packets.
        while (stream.getState() != StreamState.PLAYING) {
            System.out.println(stream.getState());
            try {
                stream.updateStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        List<Packet> polledPackets = stream.pollAllPackets();

        List<EntityPacket> entityList = new ArrayList<>();
        World world = null;
        for (Packet packet : polledPackets) {
            System.out.println("Recieved packet: "+packet.toString());
            if (packet instanceof WorldPacket) {
                world = ((WorldPacket) packet).world;
            }
            if (packet instanceof UUIDPacket) {
                playerUUID = ((UUIDPacket) packet).id;
            }
            if (packet instanceof EntityPacket) {
                entityList.add((EntityPacket) packet);
            }
        }

        ACEngine engine = GameManager.createClientEngine(this, world);
        engine.addEntityChangeListener((e, added) -> {
            engine.getSystem(ResourceSystem.class).calculateTurn(true);
        });
        for (EntityPacket entityPacket : entityList) {
            if (entityPacket.added) {
                engine.addEntity(entityPacket.entity);
                BuildingComponent bc = entityPacket.entity.getComponent(BuildingComponent.class);
                OwnerComponent oc = entityPacket.entity.getComponent(OwnerComponent.class);
                if (bc != null && bc.buildingId.equals("HEADQUARTERS") && oc != null && oc.playerID.equals(playerUUID.toString())) {
                    TilePositionComponent tpc = entityPacket.entity.getComponent(TilePositionComponent.class);
                    focusOnTile(tpc.tileX, tpc.tileY);
                }
            } else {
                engine.removeEntity(entityPacket.entity);
            }
        }
        gameManager = new GameManager(stream, engine, world);

        /*for (int i = 0; i < 1; i++) {
            stream = new LocalStream(server, true);
            server.addClientStream((LocalStream) stream, false);

            while (stream.getState() != StreamState.PLAYING) {
                try {
                    stream.updateStream();
                    System.out.println("Updating stream, current state: " + stream.getState());
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException("Forcibly disconnected.");
                }
            }

            // Connected. Process packets.
            List<Packet> polledPackets = stream.pollAllPackets();
            List<EntityPacket> entityList = new ArrayList<>();
            World world = null;
            for (Packet packet : polledPackets) {
                if (packet instanceof WorldPacket) {
                    world = ((WorldPacket) packet).world;
                }
                if (packet instanceof UUIDPacket) {
                    playerUUID = ((UUIDPacket) packet).id;
                }
                if (packet instanceof EntityPacket) {
                    entityList.add((EntityPacket) packet);
                    //System.out.println("Recieved entity: "+((EntityPacket) packet).entity);
                }
            }

            ACEngine engine = GameManager.createClientEngine(this, world);
            engine.addEntityChangeListener((e, added) -> {
                engine.getSystem(ResourceSystem.class).calculateTurn(true);
            });
            for (EntityPacket entityPacket : entityList) {
                if (entityPacket.added) {
                    engine.addEntity(entityPacket.entity);
                    BuildingComponent bc = entityPacket.entity.getComponent(BuildingComponent.class);
                    if (bc != null && bc.buildingId.equals("HEADQUARTERS")) {
                        TilePositionComponent tpc = entityPacket.entity.getComponent(TilePositionComponent.class);
                        focusOnTile(tpc.tileX, tpc.tileY);
                    }
                } else {
                    engine.removeEntity(entityPacket.entity);
                }
            }
            if (i == 0) {      
                gameManager = new GameManager(stream, engine, world);
            }
            
        }*/
    }

    private void prepareUI() {
        uiViewport = new ExtendViewport(640, 360);
        worldViewport = new ExtendViewport(640, 360);

        skin = Sprites.getSkin();
        stage = new Stage(uiViewport, AlchemyCompany.getInstance().batch());

        setupMenus();
    }

    private void updateInput() {
        inputHelper.updatePanCamera();
        inputHelper.updateInput();
    }

    public void focusOnTile(int x, int y) {
        focusX = x;
        focusY = y;
    }

    private void processTileFocus() {
        if (focusX != -1 && focusY != -1) {
            if (worldViewport.getCamera() instanceof OrthographicCamera) {
                OrthographicCamera cam = (OrthographicCamera) worldViewport.getCamera();
                float centerX = (focusX + 0.5f);
                float centerY = (focusY + 0.5f);

                Vector3 iso = IsometricHelper.project(centerX, centerY);
                cam.position.set((iso.x) * RenderSystem.TILE_SIZE, (iso.y / 4f) * RenderSystem.TILE_SIZE, cam.position.z); // Keep current z
                cam.update();
                focusX = -1;
                focusY = -1;
            }
        }
    }

    private void checkConnection() {
        if (this.stream.getState() == StreamState.FINISHED) {
            returnToTitle();
        }
    }

    public void returnToTitle() {
        returnToTitle("Disconnected.");
    }

    public void returnToTitle(String message) {
        dispose();
        AlchemyCompany.getInstance().setScreen(new MainMenuScreen(message));
    }

    private void setupInputProcessor() {
        inputHelper = new InputHelper(this, gameManager, playerUUID, worldViewport);
        inputHelper.prepare();
    }

    private void setupMenus() {
        menuHelper = new MenuHelper(this, gameManager, playerUUID, stage);
        menuHelper.build();
    }

    // Helper methods

    public Stream getStream() {
        return stream;
    }

    boolean turnFinished = false;
    public void onTurnButtonPressed() {

    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

}
