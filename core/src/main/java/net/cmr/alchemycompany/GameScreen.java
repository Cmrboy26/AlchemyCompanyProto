package net.cmr.alchemycompany;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import net.cmr.alchemycompany.Sprites.SpriteType;

/**
 * First screen of the application. Displayed after the application is created.
 */
public class GameScreen implements Screen {

    private Viewport uiViewport;
    private Viewport worldViewport;
    private Stage stage;
    private World world;
    private int TILE_SIZE = 75;

    private int lastMouseX = -1;
    private int lastMouseY = -1;

    @Override
    public void show() {
        // Prepare your screen here.
        prepareUI();
        setupInputProcessor();
        setupWorld();
    }

    @Override
    public void render(float delta) {
        // Draw your screen here. "delta" is the time since last render in seconds.
        update(delta);
        renderWorld();
        renderUI();
    }

    @Override
    public void resize(int width, int height) {
        // If the window is minimized on a desktop (LWJGL3) platform, width and height
        // are 0, which causes problems.
        // In that case, we don't resize anything, and wait for the window to be a
        // normal size before updating.
        if (width <= 0 || height <= 0)
            return;
        uiViewport.update(width, height, true);
        worldViewport.update(width, height, true);
        // Resize your screen here. The parameters represent the new window size.
    }

    @Override
    public void pause() {
        // Invoked when your application is paused.
    }

    @Override
    public void resume() {
        // Invoked when your application is resumed after pause.
    }

    @Override
    public void hide() {
        // This method is called when another screen replaces this one.
    }

    @Override
    public void dispose() {
        // Destroy screen's assets here.
    }

    private void update(float delta) {
        // Camera drag with right mouse button
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

    private void renderWorld() {
        SpriteBatch batch = AlchemyCompany.getInstance().batch();
        worldViewport.apply();
        batch.setProjectionMatrix(worldViewport.getCamera().combined);
        batch.begin();

        for (int y = world.height - 1; y >= 0; y--) {
            for (int x = 0; x < world.width; x++) {
                World.WorldFeature feature = world.features[x][y];
                SpriteType spriteType = null;
                switch (feature) {
                    case CRYSTAL_VALLEY:
                        spriteType = SpriteType.CRYSTAL_VALLEY;
                        break;
                    case FOREST:
                        spriteType = SpriteType.FOREST;
                        break;
                    case MOUNTAINS:
                        spriteType = SpriteType.MOUNTAINS;
                        break;
                    case PLAINS:
                        spriteType = SpriteType.PLAINS;
                        break;
                    case SWAMP:
                        spriteType = SpriteType.SWAMP;
                        break;
                    default:
                        break;
                }
                Texture texture = Sprites.getTexture(spriteType);
                if (texture != null) {
                    batch.draw(texture, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                } else {
                    System.err.println("Texture for " + feature + " not found!");
                }

                World.Building building = world.buildings[x][y];
                if (building != null) {
                    SpriteType buildingSpriteType = null;
                    switch (building) {
                        case HEADQUARTERS:
                            buildingSpriteType = SpriteType.HEADQUARTERS;
                            break;
                        case EXTRACTOR:
                            buildingSpriteType = SpriteType.EXTRACTOR;
                            break;
                        case STORAGE:
                            buildingSpriteType = SpriteType.STORAGE;
                            break;
                        case FACTORY:
                            buildingSpriteType = SpriteType.FACTORY;
                            break;
                        case STATUE:
                            buildingSpriteType = SpriteType.STATUE;
                            break;
                        case RESEARCH_LAB:
                            buildingSpriteType = SpriteType.RESEARCH_LAB;
                            break;
                        case ARCHER_TOWER:
                            buildingSpriteType = SpriteType.ARCHER_TOWER;
                            break;
                        case BARRACKS:
                            buildingSpriteType = SpriteType.BARRACKS;
                            break;
                        default:
                            break;
                    }
                    Texture buildingTexture = Sprites.getTexture(buildingSpriteType);
                    if (buildingTexture != null) {
                        batch.draw(buildingTexture, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    } else {
                        
                    }
                }
            }
        }

        batch.end();
    }

    private void renderUI() {
        stage.act(); // Update the stage actors.
        stage.draw(); // Draw the stage.
    }

    private void prepareUI() {
        uiViewport = new ExtendViewport(1080, 720);
        worldViewport = new ExtendViewport(1080, 720);

        Skin skin = Sprites.getSkin();
        stage = new Stage(uiViewport, AlchemyCompany.getInstance().batch());

        Table bottomMenu = new Table(skin);
        bottomMenu.setFillParent(true);
        bottomMenu.bottom();
        stage.addActor(bottomMenu);

        int buttonSize = 50; // Size of the buttons in the bottom menu.

        ButtonGroup<TextButton> buildingGroup = new ButtonGroup<>();
        buildingGroup.setMinCheckCount(0);
        buildingGroup.setMaxCheckCount(1);

        for (int i = 0; i < World.Building.values().length; i++) {
            World.Building building = World.Building.values()[i];
            String buttonText = building.name().replace('_', ' ').toLowerCase();
            TextButton button = new TextButton(buttonText, skin, "toggle");
            //button.addListener(new BuildingButtonListener(building));
            buildingGroup.add(button);
            bottomMenu.add(button).size(buttonSize, buttonSize).pad(10);
            
        }

    }

    private void setupInputProcessor() {
        InputMultiplexer inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(stage);

        inputMultiplexer.addProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                if (worldViewport.getCamera() instanceof OrthographicCamera) {
                    OrthographicCamera cam = (OrthographicCamera) worldViewport.getCamera();
                    float oldZoom = cam.zoom;
                    float newZoom = oldZoom + amountY * 0.1f;
                    newZoom = Math.max(0.5f, Math.min(newZoom, 3f)); // Clamp zoom between 0.5 and 3

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

        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    private void setupWorld() {
        // Initialize the world with a specific type.
        world = new World(World.WorldType.SMALL);
    }

    private Vector2 getTileCoordinates(int screenX, int screenY) {
        // Convert screen coordinates to world coordinates
        Vector3 worldCoords = worldViewport.unproject(new Vector3(screenX, screenY, 0));
        int tileX = (int) (worldCoords.x / TILE_SIZE);
        int tileY = (int) (worldCoords.y / TILE_SIZE);
        return new Vector2(tileX, tileY);
    }

}