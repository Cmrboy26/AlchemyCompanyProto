package net.cmr.alchemycompany;

import java.util.HashMap;
import java.util.HashSet;

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

import net.cmr.alchemycompany.component.AvailableRecipesComponent;
import net.cmr.alchemycompany.component.ProducerComponent;
import net.cmr.alchemycompany.component.StorageComponent;
import net.cmr.alchemycompany.entity.BuildingFactory.BuildingType;
import net.cmr.alchemycompany.game.Resources.Resource;
import net.cmr.alchemycompany.system.RenderSystem;
import net.cmr.alchemycompany.system.ResourceSystem;
import net.cmr.alchemycompany.world.TilePoint;
import net.cmr.alchemycompany.world.World;
import net.cmr.alchemycompany.world.World.WorldType;

public class GameScreen implements Screen {

    private Viewport uiViewport;
    private Viewport worldViewport;
    private Stage stage;
    private Skin skin;
    private int lastMouseX, lastMouseY;

    private GameManager gameManager;

    @Override
    public void show() {
        prepareGame();
        prepareUI();
        setupInputProcessor();
    }

    @Override
    public void render(float delta) {
        // Camera drag with right mouse button
        processPanCameraInput();
        updateInput();
        gameManager.getEngine().update(delta);

        SpriteBatch batch = AlchemyCompany.getInstance().batch();
        worldViewport.apply();
        batch.setProjectionMatrix(worldViewport.getCamera().combined);
        gameManager.getEngine().render(batch, delta);
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
        
        ACEngine engine = new ACEngine();
        World world = new World(WorldType.MEDIUM, System.currentTimeMillis());
        engine.registerSystem(new RenderSystem(world));
        engine.registerSystem(new ResourceSystem());

        gameManager = new GameManager(true, engine, world);
        //System.out.println();
        // Entity entity = new Entity() {};
        // entity.addComponent(new TilePositionComponent(0, 0), gameEngine);  
    }

    private void prepareUI() {
        uiViewport = new ExtendViewport(640, 360);
        worldViewport = new ExtendViewport(640, 360);

        skin = Sprites.getSkin();
        stage = new Stage(uiViewport, AlchemyCompany.getInstance().batch());
    }

    private void updateInput() {
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            Vector3 screenCoords = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            Vector3 worldCoords = worldViewport.unproject(screenCoords);
            TilePoint tileCoords = IsometricHelper.worldToIsometricTile(worldCoords, gameManager.getWorld());
            if (tileCoords != null) {
                BuildingType selectedType = Gdx.input.isKeyPressed(Input.Keys.SPACE) ? BuildingType.FARM : BuildingType.HEADQUARTERS;
                gameManager.tryPlaceBuilding(null, selectedType, tileCoords.getX(), tileCoords.getY()); 
                gameManager.getEngine().getSystem(ResourceSystem.class).calculateTurn();
            }

            //if (tileCoords != null) gameManager.getWorld().setTileFeature(WorldFeature.SWAMP, tileCoords.getX(), tileCoords.getY());
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
    
}
