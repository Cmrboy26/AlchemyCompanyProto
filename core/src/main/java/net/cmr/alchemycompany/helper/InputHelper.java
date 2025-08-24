package net.cmr.alchemycompany.helper;

import java.util.UUID;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.utils.viewport.Viewport;

import net.cmr.alchemycompany.GameManager;
import net.cmr.alchemycompany.GameScreen;
import net.cmr.alchemycompany.IsometricHelper;
import net.cmr.alchemycompany.system.SelectionSystem;
import net.cmr.alchemycompany.world.TilePoint;

public class InputHelper extends ScreenHelper {

    Viewport worldViewport;
    int lastMouseX = -1;
    int lastMouseY = -1;

    public InputHelper(GameScreen screen, GameManager gameManager, UUID playerUUID, Viewport worldViewport) {
        super(screen, gameManager, playerUUID);
        this.worldViewport = worldViewport;
    }

    public void prepare() {
        InputMultiplexer inputMultiplexer = new InputMultiplexer();

        inputMultiplexer.addProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                if (worldViewport.getCamera() instanceof OrthographicCamera && !screen.menuHelper.isOverUI()) {
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
        inputMultiplexer.addProcessor(screen.menuHelper.stage);

        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    public void updateInput() {
        Vector3 screenCoords = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        Vector3 worldCoords = worldViewport.unproject(screenCoords);
        TilePoint tileCoords = IsometricHelper.worldToIsometricTile(worldCoords, gameManager.getWorld());
        if (tileCoords != null) {
            if (!screen.menuHelper.isOverUI()) {
                boolean multiple = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);
                Button selectedShopButton = screen.menuHelper.shopGroup.getChecked();
                
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                    
                    if (selectedShopButton != null) {
                        String buildingId = selectedShopButton.getName();
                        gameManager.tryPlaceBuilding(playerUUID, buildingId, tileCoords.getX(), tileCoords.getY(), false);
                        if (!multiple) screen.menuHelper.shopGroup.uncheckAll();
                        // if (!multiple) screen.menuHelper.menusGroup.uncheckAll();
                    } else {
                        gameManager.getEngine().getSystem(SelectionSystem.class).select(tileCoords.getX(), tileCoords.getY());
                    }
                }
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                gameManager.getEngine().getSystem(SelectionSystem.class).deselect();
            }
        }
    }

    public void updatePanCamera() {
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
    
}
