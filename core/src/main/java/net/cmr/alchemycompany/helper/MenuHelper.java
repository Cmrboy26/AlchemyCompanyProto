package net.cmr.alchemycompany.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import net.cmr.alchemycompany.GameManager;
import net.cmr.alchemycompany.Sprites;
import net.cmr.alchemycompany.component.AttackComponent;
import net.cmr.alchemycompany.component.AvailableRecipesComponent;
import net.cmr.alchemycompany.component.ConstructionComponent;
import net.cmr.alchemycompany.component.ConsumerComponent;
import net.cmr.alchemycompany.component.HealthComponent;
import net.cmr.alchemycompany.component.LabelComponent;
import net.cmr.alchemycompany.component.MovementComponent;
import net.cmr.alchemycompany.component.ProducerComponent;
import net.cmr.alchemycompany.component.RenderComponent;
import net.cmr.alchemycompany.component.ResearchManagementComponent;
import net.cmr.alchemycompany.component.SelectedRecipeComponent;
import net.cmr.alchemycompany.component.TilePositionComponent;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.game.Recipe;
import net.cmr.alchemycompany.game.Registry;
import net.cmr.alchemycompany.game.Resource;
import net.cmr.alchemycompany.game.Resources;
import net.cmr.alchemycompany.screen.GameScreen;
import net.cmr.alchemycompany.screen.UpdatingImage;
import net.cmr.alchemycompany.screen.menus.GameMenu;
import net.cmr.alchemycompany.screen.menus.ShopMenu;
import net.cmr.alchemycompany.screen.menus.TechnologyMenu;
import net.cmr.alchemycompany.system.ResearchSystem;
import net.cmr.alchemycompany.system.ResourceSystem;
import net.cmr.alchemycompany.system.SelectionSystem;
import net.cmr.alchemycompany.system.TurnSystem;
import net.cmr.alchemycompany.world.Tile;
import net.cmr.alchemycompany.world.World.WorldFeature;

public class MenuHelper extends ScreenHelper {

    Stage stage;
    //ButtonGroup<Button> shopGroup = new ButtonGroup<>();
    ButtonGroup<TextButton> menuSelectorGroup = new ButtonGroup<>();
    TextButton endTurnButton = null;
    private Map<Class<? extends GameMenu>, GameMenu> menus = new LinkedHashMap<>();
    private boolean resourcesChanged = false;

    public MenuHelper(GameScreen screen, GameManager gameManager, UUID playerUUID, Stage stage) {
        super(screen, gameManager, playerUUID);
        this.stage = stage;
        this.menus = new LinkedHashMap<>();
        gameManager.getEngine().getSystem(ResourceSystem.class).addListener((rs) -> {
            resourcesChanged = true;
            Gdx.app.postRunnable(() -> {
                resourcesChanged = false;
            });
        });
    }

    public boolean isOverUI() {
        Vector2 stageCoords = stage.screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        return stage.hit(stageCoords.x, stageCoords.y, true) != null;
    }

    public void build() {
        Table shopMenuTable = new Table();
        shopMenuTable.left().pad(10);
        shopMenuTable.setVisible(false);
        stage.addActor(shopMenuTable);

        GameMenu shopMenu = new ShopMenu(Align.left, this);
        shopMenu.setVisible(true);
        menus.put(ShopMenu.class, shopMenu);
        shopMenuTable.add(shopMenu).left().expand().space(10);

        Table technologyMenuTable = new Table();
        technologyMenuTable.left().pad(10);
        technologyMenuTable.setVisible(false);
        stage.addActor(technologyMenuTable);

        GameMenu technologyMenu = new TechnologyMenu(Align.left, this);
        technologyMenu.setVisible(true);
        menus.put(TechnologyMenu.class, technologyMenu);
        technologyMenuTable.add(technologyMenu).left().expand().space(10);

        Table menuSelectorTable = new Table();
        menuSelectorTable.setFillParent(true);
        menuSelectorTable.left().pad(10).padLeft(0);
        stage.addActor(menuSelectorTable);

        menuSelectorGroup.setMaxCheckCount(1);
        menuSelectorGroup.setMinCheckCount(0);

        Consumer<GameMenu> onMenuClosed = (menuInstance) -> {
            menuSelectorTable.setVisible(true);
            menuSelectorTable.toFront();
            menuInstance.getParent().addAction(Actions.sequence(
                Actions.moveToAligned(0, stage.getHeight() / 2, Align.right, 0.25f, Interpolation.sineOut),
                Actions.visible(false)
            ));
        };

        for (Entry<Class<? extends GameMenu>, GameMenu> entry : menus.entrySet()) {
            Class<? extends GameMenu> menuClass = entry.getKey();
            GameMenu menuInstance = entry.getValue();
            TextButton button = new TextButton(menuInstance.name, Sprites.getSkin());
            button.getLabel().setWrap(true);
            menuInstance.setOnClose(onMenuClosed);
            button.pack();
            menuSelectorTable.add(button).width(70).pad(2);
            menuSelectorGroup.add(button);

            Table quickMessageTable = new Table(skin);
            if (menuInstance.isQuickMessageEnabled()) {
                quickMessageTable.addAction(Actions.fadeOut(0.01f));
                quickMessageTable.setBackground(skin.getDrawable("window"));
                menuSelectorTable.add(quickMessageTable).fillX().height(30);
                Label quickMessageLabel = new Label(menuInstance.getQuickMessage(), skin);
                quickMessageLabel.setName("message");
                quickMessageLabel.setFontScale(0.75f);
                quickMessageLabel.setAlignment(Align.center);
                quickMessageTable.add(quickMessageLabel).pad(4).fillX();
                InputListener inputListener = new InputListener() {
                    @Override
                    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                        if (menuSelectorTable.isVisible()) {
                            quickMessageTable.addAction(Actions.fadeIn(.1f));
                        }
                    }
                    @Override
                    public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                        if (menuSelectorTable.isVisible()) {
                            quickMessageTable.addAction(Actions.fadeOut(.1f));
                        }
                    }
                };
                quickMessageLabel.addAction(Actions.forever(Actions.run(() -> {
                    String message = menuInstance.getQuickMessage();
                    quickMessageTable.setVisible(message != null);
                    if (message != null && !message.equals(quickMessageLabel.getText().toString())) {
                        quickMessageLabel.setText(message);
                        quickMessageLabel.pack();
                        quickMessageTable.pack();
                        if (menuSelectorTable.isVisible()) {
                            quickMessageTable.clearActions();
                            quickMessageTable.addAction(Actions.sequence(
                                Actions.fadeIn(0.1f),
                                Actions.delay(0.5f),
                                Actions.fadeOut(2)
                            ));
                        }
                    }
                })));
                button.addListener(inputListener);

                quickMessageTable.pack();
            }
            button.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    openMenu();
                    return true;
                }

                private void openMenu() {
                    menuSelectorTable.setVisible(false);
                    menuInstance.getParent().addAction(Actions.sequence(
                        Actions.visible(true),
                        Actions.moveToAligned(0, stage.getHeight() / 2, Align.left, 0.25f, Interpolation.sineOut)
                    ));
                    Actor quickLabel = quickMessageTable;
                    if (quickLabel != null) {
                        quickLabel.addAction(Actions.fadeOut(.001f));
                    }
                    menuInstance.toFront();
                }
            });
            menuSelectorTable.row();
            
            
            ((Table) menuInstance.getParent()).pack();
            ((Table) menuInstance.getParent()).setPosition(0, stage.getHeight() / 2, Align.right);
        }
    
        menuSelectorTable.toBack();

        Table rightBottom = new Table();
        rightBottom.setFillParent(true);
        rightBottom.right().bottom().pad(10);
        createTurnMenu(rightBottom);

        Table topMenu = new Table();
        topMenu.setFillParent(true);
        topMenu.top();
        stage.addActor(topMenu);

        Table hudTable = new Table(skin);
        //hudTable.setBackground(skin.getDrawable("window"));
        Label turnLabel = new Label("Turn: " + screen.getTurn(), skin);
        topMenu.add(hudTable).growX().right().space(8);
        hudTable.addAction(Actions.forever(Actions.run(() -> {
            if (haveResourcesChanged()) {
                hudTable.clearChildren();
                hudTable.pack();
                
                turnLabel.setFontScale(0.75f);
                hudTable.pad(4);

                hudTable.add(turnLabel).growX().left().space(8);
                populateResourceTable(hudTable, false, (resId -> {
                    return resId.equals("GOLD") || resId.equals("SCIENCE");
                }));
            }
        })));
        hudTable.pack();

        turnLabel.setFontScale(0.75f);
        hudTable.pad(4);

        hudTable.add(turnLabel).growX().left().space(8);
        populateResourceTable(hudTable, false, (resId -> {
            return resId.equals("GOLD") || resId.equals("SCIENCE");
        }));

        Table miscResourcesTable = new Table(skin);
        miscResourcesTable.setBackground(skin.getDrawable("window"));
        miscResourcesTable.pad(4);
        topMenu.add(miscResourcesTable).right().space(8);
        populateResourceTable(miscResourcesTable, true, (resId -> {
            return !resId.equals("GOLD") && !resId.equals("SCIENCE");
        }));

        createSelectTable();

        stage.addAction(Actions.forever(Actions.run(() -> {
            if (haveResourcesChanged()) {
                miscResourcesTable.clearChildren();
                populateResourceTable(miscResourcesTable, true, (resId -> {
                    return !resId.equals("GOLD") && !resId.equals("SCIENCE");
                }));
            }
        })));

    }

    private boolean haveResourcesChanged() {
        return resourcesChanged;
    }

    private void populateResourceTable(Table resourceTable, boolean vertical, Predicate<String> filter) {
        Map<String, Float> resourceMap = gameManager.getEngine().getSystem(ResourceSystem.class).getCachedStoredResources(playerUUID);
        Map<String, Float> productionMap = gameManager.getEngine().getSystem(ResourceSystem.class).getCachedResourcePerSecond(playerUUID);
        Map<String, Float> totalStorageMap = gameManager.getEngine().getSystem(ResourceSystem.class).getCachedTotalStorageCapacity(playerUUID);
        Set<String> validResourceKeys = new HashSet<>();
        resourceMap.entrySet().forEach(entry -> {
            if (entry.getValue() != 0) validResourceKeys.add(entry.getKey());
        });
        productionMap.entrySet().forEach(entry -> {
            if (entry.getValue() != 0) validResourceKeys.add(entry.getKey());
        });
        for (String resourceID : validResourceKeys) {
            if (filter != null && !filter.test(resourceID)) continue;
            Resource resource = Registry.getInstance().getRegistry(Resource.class).get(resourceID);
            float storedAmount = resourceMap.getOrDefault(resourceID, 0f);
            if (resource.isPerTurnResource()) {
                storedAmount = -1;
            }

            Table resourceSection = Resources.createResourceTable(resourceID, 12, storedAmount, totalStorageMap.get(resourceID), productionMap.get(resourceID), false);
            resourceTable.add(resourceSection).right().fillX();
            if (vertical) { resourceTable.row(); } else { resourceTable.add("").width(8); }
        }
        resourceTable.pack();
        
    }

    private void createTurnMenu(Table rightBottom) {
        Table turnTable = new Table(skin);
        turnTable.setBackground(skin.getDrawable("window"));
        turnTable.pad(4);
        rightBottom.add(turnTable).right().bottom().space(10);
        stage.addActor(rightBottom);

        Callable<Integer> turnNumberCallable = () -> {
            return screen.getTurn();
        };
        Label turnLabel = new Label("Turn: ...", skin);
        turnLabel.setFontScale(0.75f);
        turnLabel.addAction(Actions.forever(Actions.run(() -> {
            try {
                turnLabel.setText("Turn: " + turnNumberCallable.call());
            } catch (Exception e) {
                e.printStackTrace();
            }
        })));
        turnTable.add(turnLabel).pad(2).row();

        endTurnButton = new TextButton("End Turn", skin);
        endTurnButton.getLabel().setWrap(true);
        endTurnButton.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (endTurnButton.isDisabled()) return false;
                TurnSystem.broadcastTurnState(endTurnButton.isPressed(), playerUUID, screen.getStream());
                return true;
            }
        });

        turnTable.add(endTurnButton).width(100).height(50).pad(2).row();
    }

    Consumer<UUID> populateSelectionTable = null;

    private void createSelectTable() {
        Table leftBottom = new Table();
        leftBottom.setFillParent(true);
        leftBottom.left().bottom().pad(10);
        stage.addActor(leftBottom);

        Table selectTable = new Table(skin);
        selectTable.setBackground(skin.getDrawable("window"));
        selectTable.pad(4);
        leftBottom.add(selectTable).left().bottom().space(10);

        populateSelectionTable = (uuid) -> {
            selectTable.clearChildren();
            String selectedName = uuid != null ? uuid.toString() : null;
            selectTable.setName(selectedName);
            Entity entity = gameManager.getEngine().getEntity(uuid);
            if (uuid == null || entity == null) {
                selectTable.pack();
                selectTable.setVisible(false);
                return;
            }
            selectTable.setVisible(true);

            Table displayTable = new Table();
            selectTable.add(displayTable).pad(2);
            Table infoTable = new Table();
            selectTable.add(infoTable).pad(2);
            Table unitLikeTable = new Table();
            selectTable.add(unitLikeTable).pad(2);
            Table constructionTable = new Table();
            selectTable.add(constructionTable).pad(2);
            Table recipeTable = new Table();
            selectTable.add(recipeTable).pad(2);

            TilePositionComponent tpc = entity.getComponent(TilePositionComponent.class);
            Tile tileAt = tpc != null ? gameManager.getWorld().getTile(tpc.tileX, tpc.tileY) : null;

            LabelComponent lc = entity.getComponent(LabelComponent.class);
            if (lc != null) {
                Label nameLabel = new Label(lc.name, skin);
                nameLabel.setFontScale(0.75f);
                infoTable.add(nameLabel).left().growX().pad(2).row();
                Label descLabel = new Label(lc.description, skin, "dark");
                descLabel.setFontScale(0.5f);
                descLabel.setWrap(true);
                infoTable.add(descLabel).left().width(75).pad(2).row();
                Table descriptionTooltipTable = new Table(skin);
                descriptionTooltipTable.setBackground(skin.getDrawable("window"));
                descriptionTooltipTable.pad(4);
            }
            RenderComponent rc = entity.getComponent(RenderComponent.class);
            if (rc != null) {
                UpdatingImage image = new UpdatingImage(Sprites.createUpdatingImageProvider(rc));
                image.setScaling(Scaling.fit);
                image.setAlign(Align.center);
                image.setColor(1, 1, 1, 1);
                displayTable.add(image).size(40).pad(2).row();
            }
            MovementComponent mc = entity.getComponent(MovementComponent.class);
            if (mc != null) {
                Label movementLabel = new Label("", skin);
                movementLabel.setFontScale(0.75f);
                Image movementImage = new Image(Sprites.getDrawable("movement"));
                movementImage.setColor(1, 1, 1, 1);
                unitLikeTable.add(movementImage).left().size(12).pad(1);
                unitLikeTable.add(movementLabel).left().pad(1).row();
                movementLabel.addAction(Actions.forever(Actions.run(() -> {
                    Entity updatedEntity = gameManager.getEngine().getEntity(uuid);
                    if (updatedEntity == null) {
                        populateSelectionTable.accept(uuid);
                        return;
                    }
                    MovementComponent updatedMC = updatedEntity.getComponent(MovementComponent.class);
                    if (updatedMC == null) {
                        populateSelectionTable.accept(uuid);
                        return;
                    }
                    String newText = String.format("%o / %o", (int) updatedMC.movesRemaining, (int) updatedMC.movesPerTurn);
                    movementLabel.setText(newText);
                })));
            }
            HealthComponent hc = entity.getComponent(HealthComponent.class);
            AttackComponent ac = entity.getComponent(AttackComponent.class);
            if (hc != null) {
                float healthPercent = hc.health / hc.maxHealth;
                Label healthLabel = new Label(hc.health.intValue() + " / " + hc.maxHealth.intValue(), skin);
                healthLabel.setFontScale(0.75f);
                Image healthImage = new Image(Sprites.getDrawable("hp"));
                healthImage.setColor(1 - healthPercent, healthPercent, 0, 1);
                unitLikeTable.add(healthImage).left().size(12).pad(1);
                unitLikeTable.add(healthLabel).left().pad(1).row();

                if (ac != null) {
                    Label attackLabel = new Label((int) ac.baseAttack + " | " + ac.attackType.name(), skin);
                    attackLabel.setFontScale(0.75f);
                    Image attackImage = new Image(Sprites.getDrawable("strength"));
                    attackImage.setColor(1, 1, 1, 1);
                    unitLikeTable.add(attackImage).left().size(12).pad(1);
                    unitLikeTable.add(attackLabel).left().pad(1).row();
                }
            }
            ConstructionComponent cc = entity.getComponent(ConstructionComponent.class);
            if (cc != null) {
                float turnsRemaining = cc.turns;
                Label constructionLabel = new Label("Constructing...", skin);
                Image constructionImage = new Image(Sprites.getDrawable("construction"));
                Image timeImage = new Image(Sprites.getDrawable("time"));
                Label turnsLabel = new Label((int) turnsRemaining + " turn(s)", skin);
                constructionLabel.setFontScale(.75f);
                turnsLabel.setFontScale(0.75f);
                constructionTable.add(constructionImage).left().size(12);
                constructionTable.add(constructionLabel).left().pad(2).row();
                constructionTable.add(timeImage).left().size(12);
                constructionTable.add(turnsLabel).left().pad(2).row();
                constructionTable.addAction(Actions.forever(Actions.run(() -> {
                    ConstructionComponent updatedCC = entity.getComponent(ConstructionComponent.class);
                    if (updatedCC == null) {
                        populateSelectionTable.accept(uuid);
                        return;
                    }
                    if (turnsRemaining != updatedCC.turns) {
                        turnsLabel.setText((int) updatedCC.turns + " turn(s)");
                    }
                })));
                return;
            }
            AvailableRecipesComponent arc = entity.getComponent(AvailableRecipesComponent.class);
            SelectedRecipeComponent src = entity.getComponent(SelectedRecipeComponent.class);
            if (arc != null) {
                Label recipeLabel = new Label("Recipe: ", skin);
                recipeLabel.setFontScale(0.75f);
                recipeTable.add(recipeLabel).left().pad(2).row();

                ArrayList<String> recipeOptions = new ArrayList<>();
                ArrayList<Recipe> recipeIds = new ArrayList<>();
                recipeOptions.add("None");
                recipeIds.add(null);
                WorldFeature featureAt = tileAt != null ? tileAt.getFeature() : null;

                ResearchSystem researchSystem = gameManager.getEngine().getSystem(ResearchSystem.class);
                ResearchManagementComponent rmc = researchSystem.getPlayerResearchManager(playerUUID.toString());
                for (String recipeId : arc.getAvailableRecipes(featureAt)) {
                    Recipe recipe = Registry.getInstance().getRegistry(Recipe.class).get(recipeId);
                    if (recipe == null) continue;
                    
                    if (rmc.hasResearched(recipe.getRequiredTechnologies())) {
                        recipeOptions.add(recipe.getName());
                        recipeIds.add(recipe);
                    }
                }

                SelectBox<String> recipeSelectBox = new SelectBox<>(skin);
                recipeSelectBox.setItems(recipeOptions.toArray(new String[0]));
                if (src != null) {
                    String recipeId = src.selectedRecipe;
                    Recipe recipe = Registry.getInstance().getRegistry(Recipe.class).getOrDefault(recipeId, null);
                    int index = recipeIds.indexOf(recipe);
                    System.out.println(index);
                    if (index < 0) index = 0;
                    recipeSelectBox.setSelectedIndex(index);
                }
                recipeSelectBox.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        Recipe selectedRecipe = recipeIds.get(recipeSelectBox.getSelectedIndex());
                        String id = selectedRecipe != null ? selectedRecipe.getId() : null;
                        gameManager.trySelectRecipe(uuid, id, entity.getID());
                        populateSelectionTable.accept(uuid);
                    }
                });

                recipeTable.add(recipeSelectBox).width(100).pad(2).row();
                recipeSelectBox.addAction(Actions.forever(Actions.run(() -> {
                    Entity newEntity = gameManager.getEngine().getEntity(uuid);
                    if (newEntity == null) {
                        populateSelectionTable.accept(uuid);
                        return;
                    }
                    SelectedRecipeComponent updatedSRC = newEntity.getComponent(SelectedRecipeComponent.class);
                    if (updatedSRC == null) {
                        return;
                    }
                    int selectedIndex = recipeSelectBox.getSelectedIndex();
                    Recipe srcRecipe = updatedSRC.selectedRecipe != null ? Registry.getInstance().getRegistry(Recipe.class).get(updatedSRC.selectedRecipe) : null;
                    int newSelectedIndex = recipeIds.indexOf(srcRecipe);
                    if (selectedIndex != newSelectedIndex && newSelectedIndex >= 0) {
                        recipeSelectBox.setSelectedIndex(newSelectedIndex);
                        populateSelectionTable.accept(uuid);
                    }
                })));
            }
            ProducerComponent pc = entity.getComponent(ProducerComponent.class);
            ConsumerComponent cnc = entity.getComponent(ConsumerComponent.class);
            if (pc != null || cnc != null) {
                Table resourcesTable = new Table(skin);
                selectTable.add(resourcesTable).colspan(4).pad(2).row();
                // only say pc and cnc are both present if they are not null AND have something to show
                boolean pcHasProduction = pc != null && pc.production != null && pc.production.size() > 0;
                boolean cncHasConsumption = cnc != null && cnc.consumption != null && cnc.consumption.size() > 0;
                boolean bothPresent = pcHasProduction && cncHasConsumption;

                if (cncHasConsumption) {
                    Table consumptionTable = new Table();
                    resourcesTable.add(consumptionTable).pad(2);
                    Label consumptionLabel = new Label("Consumes:", skin);
                    consumptionLabel.setFontScale(0.70f);
                    consumptionTable.add(consumptionLabel).left().growX().pad(2).row();
                    int count = 0;
                    Table rowTable = new Table();
                    for (Entry<String, Float> entry : cnc.consumption.entrySet()) {
                        Table resourceEntry = Resources.createResourceTable(entry.getKey(), 12, null, null, -entry.getValue(), !bothPresent);
                        rowTable.add(resourceEntry).pad(1);
                        count++;
                        if (count % 2 == 0) {
                            consumptionTable.add(rowTable).row();
                            rowTable = new Table();
                        }
                    }
                    if (count % 2 != 0) {
                        consumptionTable.add(rowTable).row();
                    }
                }
                if (bothPresent) {
                    resourcesTable.add(new Label(" -> ", skin)).pad(2).growY().center();
                }
                if (pcHasProduction) {
                    Table productionTable = new Table();
                    resourcesTable.add(productionTable).pad(2);
                    Label productionLabel = new Label("Produces:", skin);
                    productionLabel.setFontScale(0.70f);
                    productionTable.add(productionLabel).left().growX().pad(2).row();
                    int count = 0;
                    Table rowTable = new Table();
                    for (Entry<String, Float> entry : pc.production.entrySet()) {
                        Table resourceEntry = Resources.createResourceTable(entry.getKey(), 12, null, null, entry.getValue(), !bothPresent);
                        rowTable.add(resourceEntry).pad(1);
                        count++;
                        if (count % 2 == 0) {
                            productionTable.add(rowTable).row();
                            rowTable = new Table();
                        }
                    }
                    if (count % 2 != 0) {
                        productionTable.add(rowTable).row();
                    }
                }
            }
        };

        final SelectionSystem selectionSystem = gameManager.getEngine().getSystem(SelectionSystem.class);
        selectTable.addAction(Actions.forever(Actions.run(() -> {
            UUID selectedId = selectionSystem.getSelectedId();
            String selectedName = selectedId != null ? selectedId.toString() : null;
            if (Objects.equals(selectTable.getName(), selectedName)) {
                return;
            }
            populateSelectionTable.accept(selectedId);
        })));
        populateSelectionTable.accept(selectionSystem.getSelectedId());
    }

    public <T extends GameMenu> T getMenu(Class<T> menuClass) {
        return menuClass.cast(menus.get(menuClass));
    }

    public TextButton getEndTurnButton() {
        return endTurnButton;
    }

}
