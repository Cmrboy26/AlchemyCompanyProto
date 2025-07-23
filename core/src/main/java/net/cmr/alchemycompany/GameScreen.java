package net.cmr.alchemycompany;

import java.util.function.Consumer;

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
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import net.cmr.alchemycompany.Building.ArcherTowerBuilding;
import net.cmr.alchemycompany.Building.BarracksBuilding;
import net.cmr.alchemycompany.Building.BuildingConstructorFunction;
import net.cmr.alchemycompany.Building.BuildingContext;
import net.cmr.alchemycompany.Building.ExtractorBuilding;
import net.cmr.alchemycompany.Building.FactoryBuilding;
import net.cmr.alchemycompany.Building.HeadquarterBuilding;
import net.cmr.alchemycompany.Building.ResearchLabBuilding;
import net.cmr.alchemycompany.Building.StatueBuilding;
import net.cmr.alchemycompany.Building.StorageBuilding;
import net.cmr.alchemycompany.Sprites.SpriteType;
import net.cmr.alchemycompany.World.WorldFeature;
import net.cmr.alchemycompany.World.WorldType;

/**
 * First screen of the application. Displayed after the application is created.
 */
public class GameScreen implements Screen {

    private Viewport uiViewport;
    private Viewport worldViewport;
    private Stage stage;
    private World world;
    private Skin skin;
    private int TILE_SIZE = 100;
    private ButtonGroup<BuildingButton> buildingsGroup;
    private Player player;

    private int lastMouseX = -1;
    private int lastMouseY = -1;

    @Override
    public void show() {
        // Prepare your screen here.
        player = new Player("Player1");
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
        if (width <= 0 || height <= 0) {
            return;
        }
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

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && !isOverUI()) {
            Vector2 tile = getTileCoordinates(Gdx.input.getX(), Gdx.input.getY());
            int x = (int) tile.x;
            int y = (int) tile.y;
            if (tile.y < world.height && tile.y >= 0 && tile.x < world.width && tile.x >= 0) {
                int selectedIndex = buildingsGroup.getCheckedIndex();
                if (selectedIndex != -1) {
                    boolean buttonPressed = false;
                    for (TextButton button : buildingsGroup.getButtons()) {
                        buttonPressed |= button.isPressed();
                    }
                    if (!buttonPressed) {
                        BuildingContext context = new BuildingContext(player, x, y);
                        BuildingButton button = buildingsGroup.getChecked();
                        Building building = button.createBuilding(context);

                        boolean canPlace = false;
                        WorldFeature below = world.features[x][y];
                        for (WorldFeature feature : building.getAllowedTiles()) {
                            if (below.equals(feature)) {
                                canPlace = true;
                                break;
                            }
                        }
                        if (canPlace) {
                            world.addBuilding(building);
                            if (!Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                                buildingsGroup.getChecked().setChecked(false);
                            }
                        }
                    }
                } else {
                    Building buildingAt = world.getBuilding(x, y);
                    if (buildingAt != null) {
                        for (Actor actor : stage.getActors()) {
                            if (actor.getName() != null && actor.getName().equals("building_popup")) {
                                actor.remove();
                            }
                        }
                        Window popupTable = new Window("Building", skin);
                        popupTable.setName("building_popup");
                        popupTable.add(buildingAt.onClick(skin)).fill().expand().padBottom(5).row();
                        TextButton closeButton = new TextButton("Close", skin);
                        closeButton.pad(0, 10, 0, 10);
                        closeButton.addListener(new InputListener() {
                            @Override
                            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                                popupTable.addAction(new SequenceAction(Actions.delay(0.1f), Actions.removeActor()));
                                return true;
                            }
                        });
                        popupTable.add(closeButton).align(Align.center).expand().row();
                        popupTable.pad(20);
                        popupTable.pack();
                        popupTable.setKeepWithinStage(true);
                        popupTable.setOrigin(Align.center);
                        popupTable.setPosition(stage.getWidth() / 2, stage.getHeight() / 2, Align.center);
                        stage.addActor(popupTable);
                    }
                }
            }
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.MIDDLE)) {
            focusOnTile(player.getHQPosition());
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
                    case WATER:
                        spriteType = SpriteType.WATER;
                        break;
                    default:
                        break;
                }
                Texture texture = Sprites.getTexture(spriteType);
                if (texture != null) {
                    batch.draw(texture, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }

                Building building = world.getBuilding(x, y);
                if (building != null) {
                    building.renderTile(batch, TILE_SIZE);
                }
            }
        }

        batch.end();
    }

    private void renderUI() {
        uiViewport.apply();
        stage.act(); // Update the stage actors.
        stage.draw(); // Draw the stage.
    }

    private void prepareUI() {
        uiViewport = new ExtendViewport(1080, 720);
        worldViewport = new ExtendViewport(1080, 720);

        skin = Sprites.getSkin();
        stage = new Stage(uiViewport, AlchemyCompany.getInstance().batch());

        Table bottomMenu = new Table(skin);
        bottomMenu.setFillParent(true);
        bottomMenu.bottom();
        stage.addActor(bottomMenu);

        int buttonSize = 70; // Size of the buttons in the bottom menu.

        buildingsGroup = new ButtonGroup<>();
        buildingsGroup.setMinCheckCount(0);
        buildingsGroup.setMaxCheckCount(1);

        Consumer<BuildingButton> addToMenu = (BuildingButton b) -> {
            buildingsGroup.add(b);
            bottomMenu.add(b).size(buttonSize, buttonSize).pad(10);
        };

        //addToMenu.accept(new BuildingButton(skin, "HQ", HeadquarterBuilding::new));
        addToMenu.accept(new BuildingButton(skin, "Extractor", ExtractorBuilding::new));
        addToMenu.accept(new BuildingButton(skin, "Storage", StorageBuilding::new));
        addToMenu.accept(new BuildingButton(skin, "Factory", FactoryBuilding::new));
        addToMenu.accept(new BuildingButton(skin, "Statue", StatueBuilding::new));
        addToMenu.accept(new BuildingButton(skin, "Research\nLab", ResearchLabBuilding::new));
        addToMenu.accept(new BuildingButton(skin, "Archer\nTower", ArcherTowerBuilding::new));
        addToMenu.accept(new BuildingButton(skin, "Barracks", BarracksBuilding::new));

    }

    private class BuildingButton extends TextButton {

        private final BuildingConstructorFunction bcf;

        public BuildingButton(Skin skin, String name, BuildingConstructorFunction bcf) {
            super(name, skin, "toggle");
            this.bcf = bcf;
        }

        public Building createBuilding(BuildingContext context) {
            return bcf.apply(context);
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

        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    private void setupWorld() {
        // Initialize the world with a specific type.
        world = new World(World.WorldType.MEDIUM, System.currentTimeMillis());
        world.placeHeadquarters(player);
    }

    private Vector2 getTileCoordinates(int screenX, int screenY) {
        // Convert screen coordinates to world coordinates
        Vector3 worldCoords = worldViewport.unproject(new Vector3(screenX, screenY, 0));
        int tileX = (int) (worldCoords.x / TILE_SIZE);
        int tileY = (int) (worldCoords.y / TILE_SIZE);
        return new Vector2(tileX, tileY);
    }

    public boolean isOverUI() {
        Vector2 stageCoords = stage.screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        return stage.hit(stageCoords.x, stageCoords.y, true) != null;
    }

    public void focusOnTile(Vector2 vector) {
        focusOnTile((int) vector.x, (int) vector.y);
    }

    public void focusOnTile(int x, int y) {
        if (worldViewport.getCamera() instanceof OrthographicCamera) {
            OrthographicCamera cam = (OrthographicCamera) worldViewport.getCamera();
            float centerX = (x + 0.5f) * TILE_SIZE;
            float centerY = (y + 0.5f) * TILE_SIZE;
            cam.position.set(centerX, centerY, 0);
            cam.update();
        }
    }

}
