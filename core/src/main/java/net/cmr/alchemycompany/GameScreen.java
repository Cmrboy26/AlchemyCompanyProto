package net.cmr.alchemycompany;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import net.cmr.alchemycompany.Sprites.SpriteType;
import net.cmr.alchemycompany.component.AvailableRecipesComponent;
import net.cmr.alchemycompany.component.BuildingComponent;
import net.cmr.alchemycompany.component.SelectedRecipeComponent;
import net.cmr.alchemycompany.component.TilePositionComponent;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.ecs.Family;
import net.cmr.alchemycompany.entity.BuildingFactory.BuildingType;
import net.cmr.alchemycompany.game.Recipe;
import net.cmr.alchemycompany.game.Resources;
import net.cmr.alchemycompany.network.GameServer;
import net.cmr.alchemycompany.network.LocalStream;
import net.cmr.alchemycompany.network.Stream;
import net.cmr.alchemycompany.network.Stream.StreamState;
import net.cmr.alchemycompany.network.packet.EntityPacket;
import net.cmr.alchemycompany.network.packet.Packet;
import net.cmr.alchemycompany.network.packet.UUIDPacket;
import net.cmr.alchemycompany.network.packet.WorldPacket;
import net.cmr.alchemycompany.system.RenderSystem;
import net.cmr.alchemycompany.world.TilePoint;
import net.cmr.alchemycompany.world.World;

public class GameScreen implements Screen {

    private Viewport uiViewport;
    private Viewport worldViewport;
    private Stage stage;
    private Skin skin;
    private int lastMouseX, lastMouseY;
    private UUID playerUUID;
    private int focusX = -1, focusY = -1;

    private GameManager gameManager;
    private Stream stream;

    @Override
    public void show() {
        Json json = new Json();


        prepareGame();
        prepareUI();
        setupInputProcessor();
    }

    @Override
    public void render(float delta) {
        // Camera drag with right mouse button
        processTileFocus();
        processPanCameraInput();
        updateInput();
        gameManager.getEngine().update(delta);
        List<Packet> polledPackets = stream.pollAllPackets();
        for (Packet packet : polledPackets) {
            if (packet instanceof EntityPacket) {
                EntityPacket ep = (EntityPacket) packet;
                Entity entity = ep.entity;
                boolean added = ep.added;
                System.out.println("[DEBUG] Client recieved "+entity.toShortString());
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
        gameManager.getEngine().render(playerUUID, batch, delta);
    }

    @Override
    public void resize(int width, int height) {
        worldViewport.update(width, height);
        uiViewport.update(width, height);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        stage.dispose();
    }

    private void prepareGame() {
        final GameServer server = new GameServer();
        Thread serverThread = new Thread(() -> {
            while (true) {
                server.update();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        stream = new LocalStream(server, true);
        server.addClientStream((LocalStream) stream);

        while (stream.getState() != StreamState.PLAYING) {
            try {
                stream.updateStream();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Forcibly disconnected.");
            }
        }

        // Connected. Process packets.
        List<Packet> polledPackets = stream.pollAllPackets();
        List<Entity> entityList = new ArrayList<>();
        World world = null;
        for (Packet packet : polledPackets) {
            if (packet instanceof WorldPacket) {
                world = ((WorldPacket) packet).world;
            }
            if (packet instanceof UUIDPacket) {
                playerUUID = ((UUIDPacket) packet).id;
            }
            if (packet instanceof EntityPacket) {
                entityList.add(((EntityPacket) packet).entity);
                //System.out.println("Recieved entity: "+((EntityPacket) packet).entity);
            }
        }

        ACEngine engine = GameManager.createClientEngine(world);
        for (Entity entity : entityList) {
            engine.addEntity(entity);
        }
        gameManager = new GameManager(stream, engine, world);
    }

    private void prepareUI() {
        uiViewport = new ExtendViewport(640, 360);
        worldViewport = new ExtendViewport(640, 360);

        skin = Sprites.getSkin();
        stage = new Stage(uiViewport, AlchemyCompany.getInstance().batch());
    }

    private void updateInput() {
        Vector3 screenCoords = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        Vector3 worldCoords = worldViewport.unproject(screenCoords);
        TilePoint tileCoords = IsometricHelper.worldToIsometricTile(worldCoords, gameManager.getWorld());
        if (tileCoords != null) {
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                BuildingType selectedType = BuildingType.FARM;
                gameManager.tryPlaceBuilding(playerUUID, selectedType, tileCoords.getX(), tileCoords.getY(), false);
                // TEMPORARY
            }
            if (Gdx.input.isButtonJustPressed(Input.Buttons.MIDDLE)) {
                gameManager.tryRemoveBuilding(playerUUID, tileCoords.getX(), tileCoords.getY());
            }

            Entity buildingAt = null;
            Family family = Family.all(TilePositionComponent.class, BuildingComponent.class);
            for (Entity entity : gameManager.getEngine().getEntities(family)) {
                TilePositionComponent pos = entity.getComponent(TilePositionComponent.class);
                if (pos != null && pos.tileX == tileCoords.getX() && pos.tileY == tileCoords.getY()) {
                    buildingAt = entity;
                    break;
                }
            }
            if (buildingAt != null) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                    if (buildingAt.hasComponent(AvailableRecipesComponent.class)) {
                        AvailableRecipesComponent arc = buildingAt.getComponent(AvailableRecipesComponent.class);
                        List<String> recipeList = new ArrayList<>();
                        for (String recipe : arc.availableRecipes) {
                            recipeList.add(recipe);
                        }

                        int index = 0;
                        System.out.println(buildingAt.hasComponent(SelectedRecipeComponent.class));
                        if (buildingAt.hasComponent(SelectedRecipeComponent.class)) {
                            SelectedRecipeComponent src = buildingAt.getComponent(SelectedRecipeComponent.class);
                            index = (recipeList.indexOf(src.selectedRecipe) + 1) % recipeList.size();
                        }
                        if (index == -1) {
                            index = 0;
                        }

                        String selectedRecipe = recipeList.get(index);
                        gameManager.trySelectRecipe(playerUUID, selectedRecipe, buildingAt.getID());
                    }
                }
            }
        }
    }

    private void processPanCameraInput() {
        if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
            // Store previous mouse position between frames
            if (lastMouseX == -1 && lastMouseY == -1) {
                lastMouseX = Gdx.input.getX();
                lastMouseY = Gdx.input.getY();
            } else {
                int currentX = Gdx.input.getX();
                int currentY = Gdx.input.getY();
                float deltaX = lastMouseX - currentX;
                float deltaY = currentY - lastMouseY; // Y is inverted in screen coords

                // Adjust camera position based on drag and camera zoom
                OrthographicCamera cam = (OrthographicCamera) worldViewport.getCamera();
                float zoom = cam.zoom;
                cam.position.add(
                        deltaX * worldViewport.getWorldWidth() / Gdx.graphics.getWidth() * zoom,
                        deltaY * worldViewport.getWorldHeight() / Gdx.graphics.getHeight() * zoom,
                        0);
                cam.update();

                lastMouseX = currentX;
                lastMouseY = currentY;
            }
        } else {
            lastMouseX = -1;
            lastMouseY = -1;
        }
    }

    private void setupInputProcessor() {
        InputMultiplexer inputMultiplexer = new InputMultiplexer();

        inputMultiplexer.addProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                if (worldViewport.getCamera() instanceof OrthographicCamera) {
                    OrthographicCamera cam = (OrthographicCamera) worldViewport.getCamera();
                    float oldZoom = cam.zoom;
                    float newZoom = oldZoom + Math.signum(amountY) * amountY * amountY * 0.1f;
                    newZoom = Math.max(0.5f, Math.min(newZoom, 5f)); // Clamp zoom between 0.5 and 5

                    // Move camera towards cursor position
                    float mouseX = Gdx.input.getX();
                    float mouseY = Gdx.input.getY();

                    // Convert screen coordinates to world coordinates before zoom
                    worldViewport.apply();
                    Vector3 before = cam.unproject(new Vector3(mouseX, mouseY, 0));

                    cam.zoom = newZoom;
                    cam.update();

                    // Convert screen coordinates to world coordinates after zoom
                    worldViewport.apply();
                    Vector3 after = cam.unproject(new Vector3(mouseX, mouseY, 0));

                    // Offset camera position so the point under the cursor stays fixed
                    cam.position.add(before.x - after.x, before.y - after.y, 0);
                    cam.update();

                    return true;
                }
                return false;
            }
        });
        inputMultiplexer.addProcessor(stage);

        Gdx.input.setInputProcessor(inputMultiplexer);
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

    // Helper methods

    

}
