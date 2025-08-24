package net.cmr.alchemycompany.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;

import net.cmr.alchemycompany.GameManager;
import net.cmr.alchemycompany.GameScreen;
import net.cmr.alchemycompany.Sprites;
import net.cmr.alchemycompany.component.AvailableRecipesComponent;
import net.cmr.alchemycompany.component.BuildingComponent;
import net.cmr.alchemycompany.component.ConstructionComponent;
import net.cmr.alchemycompany.component.ConsumerComponent;
import net.cmr.alchemycompany.component.LabelComponent;
import net.cmr.alchemycompany.component.ProducerComponent;
import net.cmr.alchemycompany.component.PurchaseCostComponent;
import net.cmr.alchemycompany.component.RenderComponent;
import net.cmr.alchemycompany.component.ResearchRequirementComponent;
import net.cmr.alchemycompany.component.SelectedRecipeComponent;
import net.cmr.alchemycompany.component.TilePositionComponent;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.entity.BuildingFactory;
import net.cmr.alchemycompany.entity.EntityUtils;
import net.cmr.alchemycompany.game.Recipe;
import net.cmr.alchemycompany.game.Registry;
import net.cmr.alchemycompany.game.Resource;
import net.cmr.alchemycompany.system.ResourceSystem;
import net.cmr.alchemycompany.system.SelectionSystem;
import net.cmr.alchemycompany.world.TilePoint;

public class MenuHelper extends ScreenHelper {

    Stage stage;
    ButtonGroup<Button> shopGroup;
    ButtonGroup<Button> menusGroup;

    public MenuHelper(GameScreen screen, GameManager gameManager, UUID playerUUID, Stage stage) {
        super(screen, gameManager, playerUUID);
        this.stage = stage;
    }

    public boolean isOverUI() {
        Vector2 stageCoords = stage.screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        return stage.hit(stageCoords.x, stageCoords.y, true) != null;
    }

    public void build() {

        // Top and right top is resource stats

        Table rightTop = new Table();
        rightTop.setFillParent(true);
        rightTop.right().top().pad(10);
        Table resourceTable = new Table(skin);
        resourceTable.setBackground(skin.getDrawable("window"));
        resourceTable.pad(10);

        resourceTable.addAction(Actions.forever(Actions.run(() -> {
            resourceTable.clearChildren();
            for (Resource resource : Registry.getResourceRegistry().values()) {
                Table resourceInfoTable = new Table(skin);
                float productionAmount = gameManager.getEngine().getSystem(ResourceSystem.class)
                        .getDisplayResourcePerSecond().getOrDefault(resource.getId(), 0f);
                // System.out.println(productionAmount + ": "+resource.getName());
                if (productionAmount == 0) {
                    continue;
                }
                String sign = productionAmount > 0 ? "+" : "";
                
                Label storageLabel = null;
                Label generationLabel = new Label(sign + productionAmount, skin);
                if (!resource.isPerTurnResource()) {
                    storageLabel = new Label("0", skin);
                    resourceInfoTable.add(storageLabel).spaceRight(2);
                }
                generationLabel.setFontScale(0.7f);
                resourceInfoTable.add(new Image(Sprites.getSprite(resource.getIcon()))).size(12).pad(2).spaceRight(2);
                resourceInfoTable.add(generationLabel);
                resourceTable.add(resourceInfoTable).row();
            }
        })));

        rightTop.add(resourceTable).top().right().expand().space(10);
        stage.addActor(rightTop);
        
        // Right bottom is turn button



        // Left is shop menu, research, any selection menu

        Table leftBottom = new Table();
        leftBottom.setFillParent(true);
        leftBottom.left().bottom().pad(10);
        Table menuSelector = new Table(skin);
        menuSelector.setBackground(skin.getDrawable("window"));
        menuSelector.pad(4);

        menusGroup = new ButtonGroup<>();
        menusGroup.setMaxCheckCount(1);
        menusGroup.setMinCheckCount(0);

        ImageButton shopButton = new ImageButton(Sprites.getDrawable("SHOP_ICON"));
        shopButton.pad(0);
        menusGroup.add(shopButton);
        menuSelector.add(shopButton).pad(2);

        leftBottom.add(menuSelector).left().bottom().expand().space(10);
        stage.addActor(leftBottom);



        Table left = new Table();
        left.setFillParent(true);
        left.left().pad(10);
        Table shopMenu = new Table(skin);
        shopMenu.setBackground(skin.getDrawable("window"));
        shopMenu.pad(10);
        shopMenu.setVisible(true);

        Label shopTitle = new Label("Construct Building", skin);
        shopTitle.setAlignment(Align.left);
        shopMenu.add(shopTitle).left().growX().pad(4).row();

        shopGroup = new ButtonGroup<>();
        shopGroup.setMinCheckCount(0);
        shopGroup.setMaxCheckCount(1);
        String[] buildingIds = new String[] {"MINES", "REFINERY"};
        
        Table shopEntries = new Table(skin);
        ScrollPane scrollEntries = new ScrollPane(shopEntries, skin);
        scrollEntries.setScrollingDisabled(true, false);
        for (String buildingId : buildingIds) {
            Button button = new Button(skin, "toggle");

            Entity building = BuildingFactory.createEmptyBuilding(buildingId);
            RenderComponent rc = building.getComponent(RenderComponent.class);
            PurchaseCostComponent pcc = building.getComponent(PurchaseCostComponent.class);
            LabelComponent lc = building.getComponent(LabelComponent.class);
            ResearchRequirementComponent rrc = building.getComponent(ResearchRequirementComponent.class);

            Label name = new Label(lc.name, skin);
            name.setAlignment(Align.center);
            button.add(name).left().growX();

            Label cost = new Label("Cost", skin);
            cost.setFontScale(0.75f);
            cost.setAlignment(Align.center);
            button.add(cost).right().growX().row();

            Image buildingImage = new Image(Sprites.getSprite(rc.spriteType));
            button.add(buildingImage).left().expandX();

            Table costTable = new Table(skin);
            final int columns = (int) Math.floor(pcc.resourceCost.size() / 3) + 1;
            int index = 0;
            for (Entry<String, Float> entry : pcc.resourceCost.entrySet()) {
                Resource resource = Registry.getResourceRegistry().get(entry.getKey());
                Float consumptionPerTurn = entry.getValue();
                Image image = new Image(Sprites.getSprite(resource.getIcon()));
                Label label = new Label(""+consumptionPerTurn, skin);
                costTable.add(label).pad(2).growX();
                costTable.add(image).size(16).growX();
                if (index % columns == columns - 1) {
                    costTable.row();
                }
                index++;
            }

            button.add(costTable).center().expandX().row();

            button.pad(3);
            button.setName(buildingId);

            button.addAction(Actions.forever(Actions.run(() -> {
                boolean technologyMet = true;
                // TODO: add technology requirements
                button.setDisabled(!technologyMet);
                button.getColor().a = technologyMet ? 1f : 0.5f;
            })));

            shopGroup.add(button);
            shopEntries.add(button).growX().spaceBottom(2).row();
        }
        shopMenu.add(scrollEntries).height(200).width(150).expandX();

        left.add(shopMenu).left().expand().space(10);
        stage.addActor(left);

        shopButton.addAction(Actions.forever(Actions.run(() -> {
            shopMenu.setVisible(shopButton.isChecked());
        })));

        // Middle bottom is selection information and battle calculations

        Table bottom = new Table();
        bottom.setFillParent(true);
        bottom.bottom().pad(10);
        Table selectionTable = new Table(skin);
        selectionTable.setBackground(skin.getDrawable("window"));
        selectionTable.pad(10);

        // TODO: Make SelectionSystem have listeners instead
        final SelectionSystem selectionSystem = gameManager.getEngine().getSystem(SelectionSystem.class);

        final Runnable[] updateDisplay = new Runnable[1];
        updateDisplay[0] = new Runnable() {
            @Override
            public void run() {
                UUID selectedEntityId = selectionSystem.getSelectedId();

                selectionTable.setVisible(true);
                selectionTable.reset();
                selectionTable.setName(selectedEntityId.toString());

                Entity selectedEntity = gameManager.getEngine().getEntity(selectedEntityId);
                LabelComponent lc = selectedEntity.getComponent(LabelComponent.class);
                TilePositionComponent tpc = selectedEntity.getComponent(TilePositionComponent.class);
                BuildingComponent bc = selectedEntity.getComponent(BuildingComponent.class);

                // Display name centered
                // Display description underneath in smaller characters
                // Display stats below that, including ownership, production, consumption
                // Display interactions, like recipe selection, destruction buttons, and
                // anything else

                ProducerComponent pc = selectedEntity.getComponent(ProducerComponent.class);
                ConsumerComponent cc = selectedEntity.getComponent(ConsumerComponent.class);
                AvailableRecipesComponent arc = selectedEntity.getComponent(AvailableRecipesComponent.class);
                SelectedRecipeComponent src = selectedEntity.getComponent(SelectedRecipeComponent.class);
                ConstructionComponent constc = selectedEntity.getComponent(ConstructionComponent.class);

                boolean underConstruction = constc != null && constc.turns > 0;
                boolean producing = gameManager.getEngine().getSystem(ResourceSystem.class).getActiveEntities()
                        .contains(selectedEntity);

                float smallFont = 0.5f;

                Label titleLable = new Label(lc.name, skin);
                Label descriptionLabel = new Label(lc.description, skin);
                Table statsTable = new Table(skin);
                Table interactionTable = new Table(skin);

                descriptionLabel.setFontScale(smallFont);

                if (!underConstruction) {
                    List<Table> statsSections = new ArrayList<>();
                    Color producingColor = new Color(1, 1, 1, producing ? 1f : 0.33f);

                    if (src != null) {
                        Table selectedRecipeTable = new Table(skin);
                        statsSections.add(selectedRecipeTable);
                        Recipe recipe = Registry.getInstance().getRegistry(Recipe.class).get(src.selectedRecipe);
                        String labelString = "Selected Recipe:\n";
                        if (recipe != null) {
                            labelString += recipe.getName() + "\n";
                        } else {
                            labelString += "None\n";
                        }
                        labelString += "Recipe " + (producing ? "ACTIVE" : "IDLE");
                        Label label = new Label(labelString, skin);
                        label.setFontScale(smallFont);
                        selectedRecipeTable.add(label);
                    }

                    if (cc != null) {
                        Table consumptionTable = new Table(skin);
                        statsSections.add(consumptionTable);
                        for (Entry<String, Float> entry : cc.consumption.entrySet()) {
                            Resource resource = Registry.getResourceRegistry().get(entry.getKey());
                            Float consumptionPerTurn = entry.getValue();
                            Image image = new Image(Sprites.getSprite(resource.getIcon()));
                            image.setColor(producingColor);
                            Label label = new Label("-" + consumptionPerTurn, skin);
                            label.setColor(producingColor);
                            consumptionTable.add(label);
                            consumptionTable.add(image).row();
                        }
                    }

                    if (pc != null) {
                        Table productionTable = new Table(skin);
                        statsSections.add(productionTable);
                        for (Entry<String, Float> entry : pc.production.entrySet()) {
                            Resource resource = Registry.getResourceRegistry().get(entry.getKey());
                            Float productionPerTurn = entry.getValue();
                            Image image = ResourceSystem.getImageDisplay(resource);
                            Label label = new Label("+" + productionPerTurn, skin);
                            label.setColor(producingColor);
                            productionTable.add(label);
                            productionTable.add(image).row();
                        }
                    }

                    for (Table table : statsSections) {
                        statsTable.add(table).pad(0, 3, 0, 3).expandY();
                    }

                    // Interactions

                    List<Table> interactionSections = new ArrayList<>();

                    if (arc != null) {
                        Table availableRecipesTable = new Table(skin);
                        interactionSections.add(availableRecipesTable);

                        SelectBoxStyle style = new SelectBoxStyle(skin.get(SelectBoxStyle.class));

                        SelectBox<String> box = new SelectBox<String>(style);
                        String[] items = new String[arc.availableRecipes.size() + 1];
                        String[] recipeIds = new String[arc.availableRecipes.size()];
                        items[0] = "None";
                        int index = 1;
                        int selectedIndex = 0;
                        for (String recipeId : arc.availableRecipes) {
                            Recipe recipe = Registry.getInstance().getRegistry(Recipe.class).get(recipeId);
                            items[index] = recipe.getName();
                            recipeIds[index - 1] = recipe.getId();
                            if (src != null && src.selectedRecipe.contentEquals(recipeId)) {
                                selectedIndex = index;
                            }
                            index++;
                        }
                        box.setItems(items);
                        box.setSelectedIndex(selectedIndex);
                        box.addListener(new ChangeListener() {
                            @Override
                            public void changed(ChangeEvent event, Actor actor) {
                                // change recipe
                                String recipeId = null;
                                if (box.getSelectedIndex() != 0) {
                                    recipeId = recipeIds[box.getSelectedIndex() - 1];
                                }
                                gameManager.trySelectRecipe(playerUUID, recipeId, selectedEntityId);
                                selectionSystem.deselect();
                                /*box.addAction(Actions.sequence(Actions.delay(0.1f), Actions.run(() -> {
                                    updateDisplay[0].run();
                                })));*/
                            }
                        });

                        availableRecipesTable.add(box);
                    }

                    if (bc != null && !bc.buildingId.equals("HEADQUARTERS")) {
                        Table destructionTable = new Table(skin);
                        interactionSections.add(destructionTable);

                        final String confirmString = "Confirm?";
                        final String defaultString = "Destroy";

                        TextButton destroyButton = new TextButton(defaultString, skin);

                        destroyButton.addListener(new InputListener() {
                            @Override
                            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                                if (!destroyButton.getText().toString().equals(confirmString)) {
                                    destroyButton.clearActions();
                                    destroyButton.setText(confirmString);
                                    destroyButton.addAction(Actions.sequence(
                                            Actions.delay(5),
                                            Actions.run(() -> {
                                                destroyButton.setText(defaultString);
                                            })));
                                } else {
                                    // Remove
                                    TilePoint tp = EntityUtils
                                            .getPosition(gameManager.getEngine().getEntity(selectedEntityId));
                                    selectionSystem.deselect();
                                    gameManager.tryRemoveBuilding(playerUUID, tp.getX(), tp.getY());
                                }
                                return true;
                            }
                        });
                        destructionTable.add(destroyButton);
                    }

                    for (Table table : interactionSections) {
                        interactionTable.add(table).pad(0, 3, 0, 3).expandY();
                    }
                } else {
                    // Display construction time
                    descriptionLabel.setText(
                            descriptionLabel.getText() + "\nConstruction Finished in " + constc.turns + " turn(s)");
                }

                int pad = 2;
                selectionTable.add(titleLable).pad(pad).expandX().row();
                selectionTable.add(descriptionLabel).pad(pad).expandX().row();
                selectionTable.add(statsTable).pad(pad).expandX().row();
                selectionTable.add(interactionTable).pad(pad).expandX().row();
            }
        };

        selectionTable.addAction(Actions.forever(Actions.run(() -> {
            UUID selectedEntityId = selectionSystem.getSelectedId();
            if (selectedEntityId == null) {
                selectionTable.reset();
                selectionTable.setVisible(false);
                selectionTable.setName(null);
                return;
            }
            if (selectedEntityId.toString().equals(selectionTable.getName())) {
                // Nothing changed
                return;
            }
            updateDisplay[0].run();
        })));
        selectionTable.setVisible(false);

        bottom.add(selectionTable).bottom().expand().space(10);
        stage.addActor(bottom);
    }

}
