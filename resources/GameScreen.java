package net.cmr.alchemycompany;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

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
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.ui.TooltipManager;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import net.cmr.alchemycompany.Building.BarracksBuilding;
import net.cmr.alchemycompany.Building.BarracksBuilding.BarracksRecipe;
import net.cmr.alchemycompany.Building.BuildingContext;
import net.cmr.alchemycompany.Building.HeadquarterBuilding;
import net.cmr.alchemycompany.BuildingAction.ConstructAction;
import net.cmr.alchemycompany.ResearchManager.Technology;
import net.cmr.alchemycompany.Resources.Resource;
import net.cmr.alchemycompany.Shop.Cost;
import net.cmr.alchemycompany.Shop.ShopOption;
import net.cmr.alchemycompany.Sprites.SpriteType;
import net.cmr.alchemycompany.World.WorldFeature;
import net.cmr.alchemycompany.troops.HealthHolder;
import net.cmr.alchemycompany.troops.Scout;
import net.cmr.alchemycompany.troops.Soldier;
import net.cmr.alchemycompany.troops.Troop;
import net.cmr.alchemycompany.troops.Troop.FightInformation;

/**
 * First screen of the application. Displayed after the application is created.
 */
public class GameScreen implements Screen {

    private Viewport uiViewport;
    private Viewport worldViewport;
    private Stage stage;
    private World world;
    private Skin skin;
    public static final int TILE_SIZE = 100;
    private ButtonGroup<BuildingButton> buildingsGroup;
    private ButtonGroup<TechnologyButton> techGroup;
    private Player player;
    private int turn = 1;
    public TextButton researchButton;
    private Label turnLabel;

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
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
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

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && !isOverUI() && !Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
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
                        BuildingContext context = new BuildingContext(world, player, x, y);
                        BuildingButton button = buildingsGroup.getChecked();
                        Building building = button.createBuilding(context);

                        boolean featureExists = false;
                        WorldFeature below = world.getFeature(x, y);
                        for (WorldFeature feature : building.getAllowedTiles()) {
                            if (below.equals(feature)) {
                                featureExists = true;
                                break;
                            }
                        }

                        boolean spotOccupied = world.getBuilding(x, y) != null;

                        boolean enoughResource = true;
                        Cost cost = null;
                        if (!Shop.EVERYTHING_FREE) {
                            cost = button.option.costFunction.apply(context);
                            if (cost == null || cost.resources.isEmpty()) {
                                enoughResource = true;
                            } else {
                                for (Resource resource : cost.resources.keySet()) {
                                    float amount = cost.resources.get(resource);
                                    if (player.displayStoredResources.getOrDefault(resource, 0f) < amount) {
                                        enoughResource = false;
                                        break;
                                    }
                                }
                            }
                            if (cost.requiredTechnology != null && !player.researchManager.isTechResearched(cost.requiredTechnology)) {
                                enoughResource = false;
                            }
                        }
                        

                        if (featureExists && enoughResource && !spotOccupied) {
                            if (!Shop.EVERYTHING_FREE) {
                                for (Resource resource : cost.resources.keySet()) {
                                    float amount = cost.resources.get(resource);
                                    player.consumeResource(resource, amount);
                                }
                            }
                            world.addBuilding(building);
                            if (!Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                                buildingsGroup.getChecked().setChecked(false);
                            }
                        }
                    }
                } else {
                    Building buildingAt = world.getBuilding(x, y);
                    final Troop troopAt = world.getTroopAt(x, y);
                    Troop troopSelected = player.selectedTroop;

                    if (troopAt != null) {
                        if (troopAt.equals(troopSelected)) {
                            player.selectedTroop = null;

                            for (Actor actor : stage.getActors()) {
                                if (actor.getName() != null && actor.getName().equals("troop_popup")) {
                                    actor.remove();
                                }
                            }
                        } else {
                            player.selectedTroop = troopAt;
                            
                            for (Actor actor : stage.getActors()) {
                                if (actor.getName() != null && actor.getName().equals("troop_popup")) {
                                    actor.remove();
                                }
                            }

                            Window popupTable = new Window("Troop", skin);
                            popupTable.setName("troop_popup");
                            popupTable.add(player.selectedTroop.onClick(skin)).fill().expand().padBottom(5).row();
                            
                            TextButton closeButton = new TextButton("Close", skin);
                            closeButton.pad(0, 10, 0, 10);
                            closeButton.addListener(new InputListener() {
                                @Override
                                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                                    popupTable.addAction(new SequenceAction(Actions.delay(0.1f), Actions.removeActor()));
                                    return true;
                                }
                            });
                            popupTable.add(closeButton).align(Align.left).expand();

                            TextButton removeButton = new TextButton("Destroy", skin);
                            removeButton.pad(0, 10, 0, 10);
                            removeButton.addListener(new InputListener() {
                                @Override
                                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                                    popupTable.addAction(new SequenceAction(Actions.delay(0.1f), Actions.removeActor()));
                                    world.getTroops().remove(troopAt);
                                    return true;
                                }
                            });
                            popupTable.add(removeButton).align(Align.right).expand();

                            popupTable.pad(20);
                            popupTable.pack();
                            popupTable.setKeepWithinStage(true);
                            popupTable.setOrigin(Align.center);
                            popupTable.setPosition(stage.getWidth() / 2, stage.getHeight(), Align.top);
                            stage.addActor(popupTable);
                        }
                    }
                    if (((troopSelected != null && troopSelected.getX() == x && troopSelected.getY() == y) || troopAt == null) && buildingAt != null) {
                        player.selectedTroop = null;
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
                        popupTable.add(closeButton).align(Align.left).expand();

                        if (!(buildingAt instanceof HeadquarterBuilding)) {
                            TextButton removeButton = new TextButton("Destroy", skin);
                            removeButton.pad(0, 10, 0, 10);
                            removeButton.addListener(new InputListener() {
                                @Override
                                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                                    popupTable.addAction(new SequenceAction(Actions.delay(0.1f), Actions.removeActor()));
                                    world.removeBuilding(buildingAt);
                                    return true;
                                }
                            });
                            popupTable.add(removeButton).align(Align.right).expand();
                        }

                        popupTable.pad(20);
                        popupTable.pack();
                        popupTable.setKeepWithinStage(true);
                        popupTable.setOrigin(Align.center);
                        popupTable.setPosition(stage.getWidth() * (11f / 16f), 0, Align.center);
                        stage.addActor(popupTable);
                    }
                }
            }
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.MIDDLE)) {
            focusOnTile(player.getHQPosition());
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.CONTROL_LEFT)) {
            nextTurn();
            turnLabel.setText("Turn: "+turn);
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT) && !Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
            Troop selectedTroop = player.selectedTroop;
            if (selectedTroop != null && selectedTroop.getMovesRemaining() > 0) {
                Vector2 tilePos = getTileCoordinates(Gdx.input.getX(), Gdx.input.getY());
                int tileX = (int) tilePos.x;
                int tileY = (int) tilePos.y;
                boolean inRangeOfAttack = selectedTroop.inRangeOfAttack(tileX, tileY);
                boolean actionDone = false;
                if (inRangeOfAttack && !selectedTroop.hasAttacked()) {
                    HealthHolder target = null;
                    Troop troopAt = world.getTroopAt(tileX, tileY);
                    Building buildingAt = world.getBuilding(tileX, tileY);

                    if (troopAt != null && !troopAt.equals(selectedTroop)) {
                        target = troopAt;
                    } else if (buildingAt != null) {
                        target = buildingAt;
                    }

                    if (target != null) {
                        FightInformation attack = selectedTroop.getAttack(target);
                        FightInformation defense = target.getDefense(attack, selectedTroop);
                        // TODO: simulate attack
                        boolean attackedDied = target.processAttack(attack, selectedTroop);

                        if (!attackedDied) {  
                            selectedTroop.processAttack(defense, target);
                        }

                        actionDone = true;
                        selectedTroop.setAttacked(true);
                        selectedTroop.setMoveCounter(0);
                    }
                }
                if (!actionDone) {
                    selectedTroop.moveTo(tileX, tileY);
                }
            }
        }
    }

    private void renderWorld() {
        SpriteBatch batch = AlchemyCompany.getInstance().batch();
        batch.setColor(com.badlogic.gdx.graphics.Color.WHITE);
        worldViewport.apply();
        batch.setProjectionMatrix(worldViewport.getCamera().combined);
        batch.begin();

        for (int y = world.height - 1; y >= 0; y--) {
            for (int x = 0; x < world.width; x++) {
                World.WorldFeature feature = world.getFeature(x, y);
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

        for (Troop troop : world.getTroops()) {
            troop.render(batch);
        }

        batch.end();
    }

    private void renderUI() {
        uiViewport.apply();
        stage.act(); // Update the stage actors.
        stage.draw(); // Draw the stage.
    }

    private void prepareUI() {
        uiViewport = new ExtendViewport(1280, 720);
        worldViewport = new ExtendViewport(1280, 720);

        skin = Sprites.getSkin();
        stage = new Stage(uiViewport, AlchemyCompany.getInstance().batch());

        int buttonSize = 70; // Size of the buttons in the bottom menu.

        // Right bottom table for buttons and turn counter

        Table rightBottomTable = new Table(skin);
        rightBottomTable.setFillParent(true);
        rightBottomTable.right().bottom();
        stage.addActor(rightBottomTable);

        Table fightSimulatorTable = new Table(skin);
        fightSimulatorTable.setBackground("window");
        fightSimulatorTable.pad(10);
        rightBottomTable.add(fightSimulatorTable).fillX().pad(10).colspan(1).row();

        Table attackerTable = new Table(skin);
        fightSimulatorTable.add(attackerTable).colspan(1).grow();
        Label attackerInfo = new Label("Name Here\n(100/100)", skin);
        attackerTable.add(attackerInfo).center().padBottom(5).row();
        attackerInfo.setAlignment(Align.center);
        Label attackerStrength = new Label("Strength: Here\nType: CORROSION", skin);
        attackerTable.add(attackerStrength).center().row();
        attackerStrength.setAlignment(Align.center);

        Label attackSymbol = new Label(">", skin);
        fightSimulatorTable.add(attackSymbol).colspan(1).pad(8).grow();

        Table defenderTable = new Table(skin);
        fightSimulatorTable.add(defenderTable).colspan(1).grow();
        Label defenderInfo = new Label("Name Here\n(100/100)", skin);
        defenderInfo.setAlignment(Align.center);
        defenderTable.add(defenderInfo).center().padBottom(8).row();
        Label defenderStrength = new Label("Strength: Here\nType: CORROSION", skin);
        defenderTable.add(defenderStrength).center().row();
        defenderStrength.setAlignment(Align.center);

        fightSimulatorTable.addAction(Actions.forever(Actions.run(() -> {
            HealthHolder attacker = player.selectedTroop;
            HealthHolder defender = null;

            Vector2 tileVector = getTileCoordinates(Gdx.input.getX(), Gdx.input.getY());
            int tileX = (int) tileVector.x;
            int tileY = (int) tileVector.y;
            if (!(tileX < 0 || tileX > world.width - 1 || tileY < 0 || tileY > world.height - 1)) {
                Troop troopAt = world.getTroopAt(tileX, tileY);
                Building buildingAt = world.getBuilding(tileX, tileY);
                if (troopAt != null && !troopAt.equals(player.selectedTroop)) {
                    defender = troopAt;
                } else if (buildingAt != null) {
                    defender = buildingAt;
                }
            }
            // Attacker

            if (attacker != null) {
                attackerTable.setVisible(true);  
                attackerInfo.setText(attacker.toString()+"\n("+attacker.getHealth()+"HP / "+attacker.getMaxHealth()+"HP)"); 
            } else {
                attackerTable.setVisible(false);
            }

            // Defender
            

            if (defender != null) {
                defenderTable.setVisible(true);  
                defenderInfo.setText(defender.toString()+"\n("+defender.getHealth()+"HP / "+defender.getMaxHealth()+"HP)"); 
            } else {
                defenderTable.setVisible(false);
            }

            if (defender != null && attacker != null) {
                // simulate
                
                FightInformation attack = attacker.getAttack(defender);
                FightInformation defense = defender.getDefense(attack, player.selectedTroop);

                attackerStrength.setVisible(true);
                attackerStrength.setText("Strength: "+attack.getStrength()+"\nType: "+attack.getAttackType()+"\n"+attacker.getHealth()+"HP -> "+Math.max(0, (attacker.getHealth() - defense.getStrength()))+"HP");
                defenderStrength.setVisible(true);
                defenderStrength.setText("Strength: "+defense.getStrength()+"\nType: "+defense.getAttackType()+"\n"+defender.getHealth()+"HP -> "+Math.max(0, (defender.getHealth() - attack.getStrength()))+"HP");
            } else {
                attackerStrength.setVisible(false);
                defenderStrength.setVisible(false);
            }
            fightSimulatorTable.setVisible(attackerTable.isVisible());

            
        })));

        Table belowTable = new Table();

        Table buttonsTable = new Table(skin);
        buttonsTable.setBackground("window");
        buttonsTable.pad(10);

        ButtonGroup<TextButton> optionsGroup = new ButtonGroup<>();
        optionsGroup.setMinCheckCount(0);
        optionsGroup.setMaxCheckCount(1);

        TextButton buildingsButton = new TextButton("Buildings", skin, "toggle");
        TextButton troopsButton = new TextButton("Troops", skin, "toggle");
        researchButton = new TextButton("Research", skin, "toggle");
        optionsGroup.add(buildingsButton, troopsButton, researchButton);
        
        buildingsButton.addListener(new InputListener() {
            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                Gdx.input.setCursorCatched(false);
            }
        });
        troopsButton.addListener(new InputListener() {
            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                Gdx.input.setCursorCatched(false);
            }
        });
        researchButton.addListener(new InputListener() {
            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                Gdx.input.setCursorCatched(false);
            }
        });

        buttonsTable.add(buildingsButton).size(buttonSize, buttonSize / 2).pad(5).row();;
        buttonsTable.add(troopsButton).size(buttonSize, buttonSize / 2).pad(5).row();
        buttonsTable.add(researchButton).size(buttonSize, buttonSize / 2).pad(5).row();
        //rightBottomTable.add(buttonsTable).pad(10).colspan(1).right();

        Table turnTable = new Table(skin);
        turnTable.setBackground("window");
        turnTable.pad(10);

        turnLabel = new Label("Turn: "+turn, skin);
        turnLabel.setAlignment(Align.left);
        turnTable.add(turnLabel).growX().pad(5).row();

        TextButton nextTurnButton = new TextButton("Next\nTurn", skin);
        nextTurnButton.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                nextTurnButton.setDisabled(true);
                nextTurn();
                nextTurnButton.setDisabled(false);
                turnLabel.setText("Turn: "+turn);
                return true;
            }
        });
        turnTable.add(nextTurnButton).size(100);

        belowTable.add(buttonsTable).pad(5);
        belowTable.add(turnTable).pad(5);
        rightBottomTable.add(belowTable).right().colspan(1);
        rightBottomTable.pack();

        prepareBuildingMenu(buildingsButton);
        prepareResearchMenu(researchButton);
        prepareTroopMenu(troopsButton);

        // Right top table for resource counters and other info

        Table rightTopTable = new Table(skin);
        rightTopTable.setFillParent(true);
        rightTopTable.right().top();
        stage.addActor(rightTopTable);

        Table resourcesTable = new Table(skin);
        resourcesTable.setBackground("window");
        resourcesTable.pad(10);

        Label key = new Label("Stored : Resource / Turn", skin);
        resourcesTable.add(key).growX().align(Align.right).pad(5).colspan(2).row();

        for (final Resource r : Resource.values()) {
            Label counter = new Label("- : -", skin) {
                @Override
                public void act(float delta) {
                    float stored = player.displayStoredResources.getOrDefault(r, 0f);
                    float rps = player.calculatedResourcePerSecond.getOrDefault(r, 0f);
                    String rpsString = "";
                    if (rps > 0) {
                        rpsString += "+";
                    }
                    rpsString += rps;
                    if (r == Resource.SCIENCE) {
                        setText("("+rpsString+")");
                    } else {
                        setText(stored + " ("+rpsString+")");
                    }

                    super.act(delta);
                }
            };
            Label label = new Label(r.name(), skin);
            resourcesTable.add(counter).growX().align(Align.left).pad(5);
            resourcesTable.add(label).growX().align(Align.left).pad(5).row();
        }

        rightTopTable.add(resourcesTable);
        rightTopTable.pack();
    }

    private void prepareBuildingMenu(final TextButton buildingsButton) {
        Table purchaseBuildingTable = new Table(skin);
        purchaseBuildingTable.setFillParent(true);
        purchaseBuildingTable.left();
        stage.addActor(purchaseBuildingTable);

        purchaseBuildingTable.addAction(Actions.forever(Actions.run(() -> {
            if (buildingsButton.isChecked()) {
                purchaseBuildingTable.setVisible(true);
            } else {
                purchaseBuildingTable.setVisible(false);
                buildingsGroup.uncheckAll();
            }
        })));

        Table backgroundTable = new Table(skin);
        backgroundTable.setBackground("window");
        backgroundTable.pad(10);
        purchaseBuildingTable.add(backgroundTable).pad(10).row();

        Table buyOptions = new Table(skin);
        buyOptions.setBackground("window");
        buyOptions.pad(10);
        backgroundTable.add(buyOptions);

        buildingsGroup = new ButtonGroup<>();
        buildingsGroup.setMinCheckCount(0);
        buildingsGroup.setMaxCheckCount(1);

        for (ShopOption option : ShopOption.values()) {
            BuildingButton button = new BuildingButton(skin, option);
            buildingsGroup.add(button);
            buyOptions.add(button).size(200, 50).pad(5).row();
        }

        Table costTable = new Table(skin);
        costTable.setBackground("window");
        costTable.left();
        purchaseBuildingTable.add(costTable).pad(10).fillX();

        Label costLabel = new Label("Cost:", skin);
        
        costTable.addAction(Actions.forever(Actions.run(() -> {

            BuildingButton selectedButton = buildingsGroup.getChecked();
            costTable.setVisible(false);
            costTable.pad(0);
            if (selectedButton != null) {
                costTable.clearChildren();
                costTable.add(costLabel).align(Align.left).pad(5).row();
                costTable.setVisible(true);
                costTable.pad(10);

                int cursorX = Gdx.input.getX();
                int cursorY = Gdx.input.getY();
                Vector2 tile = getTileCoordinates(cursorX, cursorY);
                int x = (int) tile.x;
                int y = (int) tile.y;
                BuildingContext context = new BuildingContext(world, player, x, y);
                Cost cost = selectedButton.option.costFunction.apply(context);
                if (cost == null || cost.resources.isEmpty()) {
                    costTable.add(new Label("No cost", skin)).align(Align.left).pad(5).row();
                } else {
                    for (Resource resource : cost.resources.keySet()) {
                        float amount = cost.resources.get(resource);
                        Label resourceLabel = new Label(resource.name() + ": " + amount, skin);
                        costTable.add(resourceLabel).align(Align.left).pad(5).row();
                    }
                }
                if (cost.requiredTechnology != null && !player.researchManager.isTechResearched(cost.requiredTechnology)) {
                    Label scienceLabel = new Label("Research: " + cost.requiredTechnology, skin);
                    costTable.add(scienceLabel).align(Align.left).pad(5).row();
                }
            }
        })));

    }

    private void prepareResearchMenu(final TextButton researchButton) {
        Table researchTable = new Table(skin);
        researchTable.setFillParent(true);
        researchTable.left();
        stage.addActor(researchTable);

        Table backgroundTable = new Table(skin);
        backgroundTable.setBackground("window");
        backgroundTable.pad(10);
        researchTable.add(backgroundTable).pad(10).row();

        WidgetGroup techArea = new WidgetGroup();
        techArea.setSize(200, 600);
        final int xDist = 10, yDist = 10, sizeX = 150, sizeY = 50;

        techGroup = new ButtonGroup<TechnologyButton>();
        techGroup.setMinCheckCount(0);
        techGroup.setMaxCheckCount(1);

        List<Integer> currentX = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            currentX.add(0);
        }
        for (final Technology tech : Technology.values()) {
            int x = currentX.get(tech.row);
            currentX.set(tech.row, x + 1);
            TechnologyButton button = new TechnologyButton(tech, skin);
            button.addAction(Actions.forever(Actions.run(() -> {
                boolean disable = false;
                if (player.researchManager.isTechResearched(tech)) {
                    disable = true;
                    button.setColor(com.badlogic.gdx.graphics.Color.BLACK);
                } else {
                    for (Technology technology : ResearchManager.prerequisitesMap.get(tech)) {
                        if (!player.researchManager.isTechResearched(technology)) {
                            disable = true;
                            button.setColor(com.badlogic.gdx.graphics.Color.WHITE);
                            break;
                        }
                    }
                }
                button.setDisabled(disable);
                if (disable) {
                    button.setChecked(false);
                } else {
                    button.setColor(com.badlogic.gdx.graphics.Color.WHITE);
                }
                if (button.isChecked() && player.researchManager.getQueuedTechnology() != tech && !button.isDisabled()) {
                    player.researchManager.queueTechnology(tech);
                    System.out.println("Queuing "+tech.name());
                } else if (!button.isChecked() && player.researchManager.getQueuedTechnology() == tech) {
                    player.researchManager.cancelTechnology();
                    System.out.println("Cancelling current tech");
                }
            })));
            button.setChecked(player.researchManager.getQueuedTechnology() == tech);
            techGroup.add(button);
            techArea.addActor(button);
            button.setPosition((sizeX + xDist) * x, (sizeY + yDist) * tech.row);
            button.setSize(sizeX, sizeY);
        }
        techArea.pack();

        ScrollPane scrollMenu = new ScrollPane(techArea, skin);
        backgroundTable.add(scrollMenu).growY().width(sizeX * 5).height(600).row();

        Label infoLabel = new Label("", skin);
        infoLabel.addAction(Actions.forever(Actions.run(() -> {
            if (player.researchManager.getQueuedTechnology() == null) {
                infoLabel.setText("No research queued.");
            } else {
                Technology queuedTech = player.researchManager.getQueuedTechnology();
                float progress = player.researchManager.getPercentCompletion();
                float scienceNeeded = player.researchManager.getScienceRemaining();

                infoLabel.setText((progress * 100f)+"% finished \""+queuedTech.name()+"\" ("+scienceNeeded+" science left)");
            }
        })));

        researchTable.addAction(Actions.forever(Actions.run(() -> {
            if (researchButton.isChecked()) {
                researchTable.setVisible(true);
            } else {
                researchTable.setVisible(false);
                //scrollMenu.cancelTouchFocus();
            }
        })));
        backgroundTable.add(infoLabel).growY().fillX().row();

        backgroundTable.pack();
        researchTable.pack();

    }

    private void prepareTroopMenu(final TextButton troopButton) {
        Table troopTable = new Table(skin);
        troopTable.setFillParent(true);
        troopTable.left();
        stage.addActor(troopTable);

        Table backgroundTable = new Table(skin);
        backgroundTable.setBackground("window");
        backgroundTable.pad(10);
        troopTable.add(backgroundTable).pad(10).row();

        final Table scrollTable = new Table(skin);
        scrollTable.pad(10);

        ScrollPane scroll = new ScrollPane(scrollTable, skin);
        scroll.setOverscroll(false, false);
        scroll.setScrollBarPositions(true, false);
        scroll.setScrollingDisabled(true, false);
        Table table = new Table();
        table.setFillParent(true);
        table.add(scroll).grow();
        backgroundTable.add(table).grow().height(600).row();

        backgroundTable.pack();
        troopTable.pack();

        

        troopButton.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                scrollTable.clear();
                for (final Building building : player.buildings) {
                    if (building instanceof BarracksBuilding) {
                        BarracksBuilding barracks = (BarracksBuilding) building;
                        TextButton focusButton = new TextButton(barracks.toString(), skin);
                        focusButton.addListener(new InputListener() {
                            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                                focusOnTile(building.getX(), building.getY());
                                return true;
                            };
                        });

                        SelectBox<BarracksRecipe> selectBox = new SelectBox<>(skin);
                        selectBox.setItems(BarracksRecipe.values());
                        selectBox.addAction(Actions.forever(Actions.run(() -> {
                            selectBox.setDisabled(!barracks.canFunction());
                            if (barracks.selectedRecipe != selectBox.getSelected()) {
                                selectBox.setSelected(barracks.selectedRecipe);
                            }
                        })));

                        Label turnsRemaining = new Label("Remaining Turns: "+barracks.getTroopConstructTime(), skin);
                        turnsRemaining.addAction(Actions.forever(Actions.run(() -> {
                            turnsRemaining.setText("Remaining Turns: "+barracks.getTroopConstructTime());
                        })));

                        Label costs = new Label("", skin);
                        costs.setWrap(true);
                        costs.addAction(Actions.forever(Actions.run(() -> {
                            costs.setVisible(barracks.selectedRecipe != BarracksRecipe.NONE);
                        })));

                        selectBox.addListener(new ChangeListener() {
                            @Override
                            public void changed(ChangeEvent event, Actor actor) {
                                barracks.queueTroop(selectBox.getSelected());
                                String costLabel = "Cost Per Turn Queued:";
                                for (Entry<Resource, Float> entry : barracks.getConsumptionPerTurn().entrySet()) {
                                    Resource r = entry.getKey();
                                    Float f = entry.getValue();
                                    costLabel += "\n(-"+f+" "+r+")";
                                }
                                costs.setText(costLabel);
                                player.updateResourceDisplay();
                            }
                        });

                        scrollTable.add(focusButton).growX().pad(5);
                        scrollTable.add(turnsRemaining).growX().pad(5).row();
                        scrollTable.add(selectBox).growX().pad(5).colspan(2).padRight(50).row();
                        scrollTable.add(costs).growX().pad(5).colspan(2).row();
                    }
                }
                scrollTable.pack();
                return false;
            }
        });

        troopTable.addAction(Actions.forever(Actions.run(() -> {
            if (troopButton.isChecked()) {
                troopTable.setVisible(true);
            } else {
                troopTable.setVisible(false);
            }
        })));
    }

    private class BuildingButton extends TextButton {

        public final ShopOption option;

        public BuildingButton(Skin skin, ShopOption option) {
            super(option.name, skin, "toggle");
            this.option = option;

            TextTooltip tooltip = new TextTooltip(option.description, skin);
            tooltip.setInstant(true);
            TooltipManager.getInstance().initialTime = 0;
            TooltipManager.getInstance().resetTime = 0f;
            TooltipManager.getInstance().subsequentTime = 0f;
            TooltipManager.getInstance().hideAll();

            addListener(tooltip);
        }

        public Building createBuilding(BuildingContext context) {
            Building building = option.constructor.apply(context);
            building.addBuildingAction(new ConstructAction(option.turns));
            return building;
        }

    }

    private class TechnologyButton extends TextButton {

        public final Technology tech;

        public TechnologyButton(Technology tech, Skin skin) {
            super(tech.name() + "\n(" + (int) tech.cost + ")", skin, "toggle");
            this.tech = tech;
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
        inputMultiplexer.addProcessor(stage);

        Gdx.input.setInputProcessor(inputMultiplexer);
    }
    

    private void setupWorld() {
        // Initialize the world with a specific type.
        world = new World(World.WorldType.MEDIUM, System.currentTimeMillis());
        world.placeHeadquarters(player);
        player.updateResourceDisplay();
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

    public void nextTurn() {
        // Advance turn counter
        turn++;

        
        // Tick down counters
        for (Building building : player.buildings) {
            building.stepActions();
        }
        for (Troop troop : world.getTroops()) {
            troop.resetMoves();
        }


        // Check win/loss
        // Reset cooldowns
        // Tick down research, construction, unit production counters
        // Produce excess resources / turn if storage is available
        // Consume resources, store excess resources
        player.nextTurn(this);

        String winMessage = null;
        if (player.displayStoredResources.getOrDefault(Resource.GOLD, 0f) >= 100 && player.researchManager.isTechResearched(Technology.BASIC_ALCHEMY)) {
            winMessage = "YOU WON THE GAME!\n\nYou synthesized 100 gold\nand squashed the competition.";
        }
        if (player.getHQ().destroyed) {
            winMessage = "YOU LOST...\n\nYou destroyed your headquarters\nin a singleplayer game and lost.\nNice job.";
        }

        if (winMessage != null) {
            // WIN GAME!!!
            System.out.println("Won the game!");
            Table centerTable = new Table();
            centerTable.center();
            centerTable.setFillParent(true);
            stage.addActor(centerTable);

            Table winTable = new Table(skin);
            winTable.setBackground("window");
            winTable.add(winMessage).fill().pad(10).row();
            winTable.pad(10);

            TextButton replayButton = new TextButton("Restart", skin);
            replayButton.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    AlchemyCompany.getInstance().setScreen(new GameScreen());     
                    return true;
                }
            });
            winTable.add(replayButton).growX();
            
            centerTable.add(winTable);
        }
    }

}
