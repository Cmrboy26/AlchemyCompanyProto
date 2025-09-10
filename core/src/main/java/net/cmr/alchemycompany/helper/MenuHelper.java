package net.cmr.alchemycompany.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Function;

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
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Tooltip;
import com.badlogic.gdx.scenes.scene2d.ui.TooltipManager;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;

import net.cmr.alchemycompany.GameManager;
import net.cmr.alchemycompany.Sprites;
import net.cmr.alchemycompany.component.AvailableRecipesComponent;
import net.cmr.alchemycompany.component.BuildingComponent;
import net.cmr.alchemycompany.component.ConstructionComponent;
import net.cmr.alchemycompany.component.ConsumerComponent;
import net.cmr.alchemycompany.component.HealthComponent;
import net.cmr.alchemycompany.component.LabelComponent;
import net.cmr.alchemycompany.component.MovementComponent;
import net.cmr.alchemycompany.component.OwnerComponent;
import net.cmr.alchemycompany.component.ProducerComponent;
import net.cmr.alchemycompany.component.PurchaseCostComponent;
import net.cmr.alchemycompany.component.RenderComponent;
import net.cmr.alchemycompany.component.ResearchManagementComponent;
import net.cmr.alchemycompany.component.ResearchRequirementComponent;
import net.cmr.alchemycompany.component.SelectedRecipeComponent;
import net.cmr.alchemycompany.component.TilePositionComponent;
import net.cmr.alchemycompany.component.actions.PlayerActionComponent;
import net.cmr.alchemycompany.component.actions.ResearchActionComponent;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.entity.BuildingFactory;
import net.cmr.alchemycompany.entity.EntityUtils;
import net.cmr.alchemycompany.game.Recipe;
import net.cmr.alchemycompany.game.Registry;
import net.cmr.alchemycompany.game.Resource;
import net.cmr.alchemycompany.game.Resources;
import net.cmr.alchemycompany.game.Technology;
import net.cmr.alchemycompany.network.packet.EntityPacket;
import net.cmr.alchemycompany.screen.GameScreen;
import net.cmr.alchemycompany.system.ResearchSystem;
import net.cmr.alchemycompany.system.ResourceSystem;
import net.cmr.alchemycompany.system.SelectionSystem;
import net.cmr.alchemycompany.system.TurnSystem;
import net.cmr.alchemycompany.world.TilePoint;

public class MenuHelper extends ScreenHelper {

    Stage stage;
    ButtonGroup<Button> shopGroup;
    ButtonGroup<Button> menusGroup;
    TextButton endTurnButton;

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

        Table resourceTable2 = new Table(skin);
        resourceTable2.setBackground(skin.getDrawable("window"));
        resourceTable2.pad(10);

        resourceTable2.addAction(Actions.forever(Actions.run(() -> {
            resourceTable2.clearChildren();
            HashSet<Resource> resourceSet = new HashSet<>(Registry.getResourceRegistry().values());
            resourceSet.removeIf(r -> {
                return !(r.getId().equals("GOLD") || r.getId().equals("SCIENCE"));
            });
            for (Resource resource : resourceSet) {
                float storageAmount = gameManager.getEngine().getSystem(ResourceSystem.class).getCachedStoredResources(playerUUID).getOrDefault(resource.getId(), 0f);
                float productionAmount = gameManager.getEngine().getSystem(ResourceSystem.class).getDisplayResourcePerSecond(playerUUID).getOrDefault(resource.getId(), 0f);
                Table resourceInfoTable = Resources.createResourceTable(resource, productionAmount, storageAmount);
                resourceTable2.add(resourceInfoTable).space(10);
            }
        })));

        rightTop.add(resourceTable2).top().right().space(10);

        Table resourceTable = new Table(skin);
        resourceTable.setBackground(skin.getDrawable("window"));
        resourceTable.pad(10);

        resourceTable.addAction(Actions.forever(Actions.run(() -> {
            resourceTable.clearChildren();
            HashSet<Resource> resourceSet = new HashSet<>(Registry.getResourceRegistry().values());
            resourceSet.removeIf(r -> {
                return r.getId().equals("GOLD") || r.getId().equals("SCIENCE");
            });
            for (Resource resource : resourceSet) {
                float storageAmount = gameManager.getEngine().getSystem(ResourceSystem.class).getCachedStoredResources(playerUUID).getOrDefault(resource.getId(), 0f);
                float productionAmount = gameManager.getEngine().getSystem(ResourceSystem.class).getDisplayResourcePerSecond(playerUUID).getOrDefault(resource.getId(), 0f);
                Table resourceInfoTable = Resources.createResourceTable(resource, productionAmount, storageAmount);
                if (resourceInfoTable.getChildren().size != 0) {
                    resourceTable.add(resourceInfoTable).space(10).row();
                }
            }
        })));

        rightTop.add(resourceTable).top().right().space(10);

        stage.addActor(rightTop);
        
        // Right bottom is turn button

        Table rightBottom = new Table();
        rightBottom.setFillParent(true);
        rightBottom.right().bottom().pad(10);
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
        turnTable.add(turnLabel).pad(4).row();

        final String endTurnString = "End Turn";
        final String continueTurnString = "Continue Turn";

        endTurnButton = new TextButton(endTurnString, skin);
        endTurnButton.getLabel().setWrap(true);
        endTurnButton.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (endTurnButton.isDisabled()) return false;
                TurnSystem.broadcastTurnState(endTurnButton.isPressed(), playerUUID, screen.getStream());
                return true;
            }
        });
        turnTable.add(endTurnButton).size(64);

        // Left is shop menu, research, any selection menu

        Table leftBottom = new Table();
        leftBottom.setFillParent(true);
        leftBottom.left().bottom().pad(10);
        Table menuSelector = new Table(skin);
        menuSelector.setBackground(skin.getDrawable("window"));
        menuSelector.pad(4);
        menuSelector.left().bottom();

        menusGroup = new ButtonGroup<>();
        menusGroup.setMaxCheckCount(1);
        menusGroup.setMinCheckCount(0);

        ImageButton shopButton = new ImageButton(Sprites.getDrawable("SHOP_ICON"));
        shopButton.pad(0);
        menusGroup.add(shopButton);
        menuSelector.add(shopButton).pad(2);

        ImageButton researchButton = new ImageButton(Sprites.getDrawable("SCIENCE_ICON"));
        researchButton.pad(0);
        menusGroup.add(researchButton);
        menuSelector.add(researchButton).pad(2);

        // TODO: ts does not look right
        leftBottom.left().bottom();
        leftBottom.add(menuSelector).space(10);
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

        List<String> buyableBuildings = new ArrayList<>();
        for (Entry<String, Entity> entry : BuildingFactory.getRegisteredBuildingEntities().entrySet()) {
            if (entry.getValue().hasComponent(PurchaseCostComponent.class)) {
                buyableBuildings.add(entry.getKey());
            }
        }
        String[] buildingIds = new String[buyableBuildings.size()];
        for (int i = 0; i < buyableBuildings.size(); i++) {
            buildingIds[i] = buyableBuildings.get(i);
        }
        
        Table scrollTable = new Table(skin);
        Table shopEntries = new Table(skin);
        ScrollPane scrollEntries = new ScrollPane(scrollTable, skin);
        scrollEntries.setScrollingDisabled(true, false);
        scrollEntries.setOverscroll(false, false);
        scrollEntries.setFadeScrollBars(false);
        scrollTable.add(shopEntries).grow();

        // Set the preferred size of the ScrollPane to be smaller than the container to enable scrolling
        scrollEntries.setForceScroll(false, true); // Enable vertical scrolling
        scrollEntries.setScrollbarsOnTop(true);

        final Function<Set<String>, Boolean> hasResearchFunction = (Set<String> techs) -> {
            ResearchSystem system = gameManager.getEngine().getSystem(ResearchSystem.class);
            if (system == null) return false;
            for (String researchID : techs) {
                if (!system.hasTechnology(playerUUID.toString(), researchID)) {
                    return false;
                }
            }
            return true;
        };

        scrollEntries.layout();
        shopEntries.padRight(scrollEntries.getScrollBarWidth() + 10);

        // TODO: sort buildings by ease to get
        for (String buildingId : buildingIds) {
            Button button = new Button(skin, "toggle");
            button.setName(buildingId);
            shopGroup.add(button);
            shopEntries.add(button).growX().pad(2).spaceRight(4).row();

            Entity building = BuildingFactory.createEmptyBuilding(buildingId);
            RenderComponent rc = building.getComponent(RenderComponent.class);
            PurchaseCostComponent pcc = building.getComponent(PurchaseCostComponent.class);
            LabelComponent lc = building.getComponent(LabelComponent.class);
            ResearchRequirementComponent rrc = building.getComponent(ResearchRequirementComponent.class);
            ConstructionComponent cc = building.getComponent(ConstructionComponent.class);

            Table textTable = new Table();
            Label name = new Label(lc.name, skin);
            name.setAlignment(Align.center);
            textTable.add(name).grow().pad(2).row();
            Image image = new Image(Sprites.getSprite(rc.spriteType));
            button.add(image).left().pad(4).colspan(1);
            button.add(textTable).left().growX().colspan(1).padRight(scrollEntries.getScrollBarWidth() + 4);
            button.pack();

            button.addListener(new InputListener() {
                 @Override
                 public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    // Display information table
                 }
                 @Override
                 public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                     // Hide information table
                 }
            });
            button.addAction(Actions.forever(Actions.run(() -> {
                boolean technologyMet = rrc == null || hasResearchFunction.apply(rrc.technologiesRequired);
                button.setDisabled(!technologyMet);
                button.getColor().a = technologyMet ? 1f : 0.5f;
                
                Actor errorActor = button.findActor("errorActor");
                if (errorActor != null) {
                    button.getCell(errorActor).pad(0);
                    button.removeActor(errorActor);
                }

                String message = null;
                if (!technologyMet) {
                    ResearchSystem system = gameManager.getEngine().getSystem(ResearchSystem.class);
                    for (String researchID : rrc.technologiesRequired) {
                        if (!system.hasTechnology(playerUUID.toString(), researchID)) {
                            Technology tech = Registry.getInstance().getRegistry(Technology.class).get(researchID);
                            message = "Locked behind \""+tech.getName()+"\"";
                            break;
                        }
                    }
                }

                if (message != null) {
                    Label bottomLabel = new Label(message, skin);
                    bottomLabel.setAlignment(Align.left);
                    bottomLabel.setWrap(true);
                    bottomLabel.setName("errorActor");
                    bottomLabel.setFontScale(0.5f);
                    button.add(bottomLabel).growX().colspan(2).padLeft(8).padBottom(2).row();
                }

                Actor infoActor = button.findActor("infoActor");
                if (infoActor != null) {
                    button.getCell(infoActor).pad(0);
                    button.removeActor(infoActor);
                }

                if (button.isChecked()) {
                    Table infoTable = new Table(skin);
                    infoTable.setName("infoActor");
                    button.add(infoTable).grow().colspan(2).pad(2).row();
                    Label descriptionLabel = new Label(lc.description, skin);
                    descriptionLabel.setWrap(true);
                    descriptionLabel.setFontScale(0.5f);
                    infoTable.add(descriptionLabel).colspan(2).growX().padLeft(4).row();
                    Label costLabel = new Label("Cost:", skin);
                    costLabel.setAlignment(Align.center);
                    infoTable.add(costLabel).colspan(1).grow();
                    Table costTable = new Table(skin);
                    infoTable.add(costTable).colspan(1).grow().row();

                    costTable.add(cc.turns + " turn" + (cc.turns == 1 ? "" : "s")).row();
                    costLabel.setFontScale(0.75f);
                    final int columns = (int) Math.floor(pcc.getResourceCost(playerUUID, buildingId, gameManager.getEngine()).size() / 3) + 1;
                    int index = 0;
                    ResourceSystem resourceSystem = gameManager.getEngine().getSystem(ResourceSystem.class);
                    Map<String, Float> cachedStoredResources = resourceSystem.getCachedStoredResources(playerUUID);
                    for (Entry<String, Float> entry : pcc.getResourceCost(playerUUID, buildingId, gameManager.getEngine()).entrySet()) {
                        Table entryTable = getResourceSection(entry.getKey(), entry.getValue());
                        if (cachedStoredResources != null) {
                            boolean enoughResources = cachedStoredResources.getOrDefault(entry.getKey(), 0f) >= entry.getValue();
                            entryTable.findActor("image").setColor(enoughResources ? Color.WHITE : Color.GRAY);
                            entryTable.findActor("amount").setColor(enoughResources ? Color.WHITE : Color.RED);
                        }
                        costTable.add(entryTable);
                        if (index % columns == columns - 1) {
                            costTable.row();
                        }
                        index++;
                    }
                }
            })));
        }
        shopMenu.add(scrollEntries).height(200).width(200).expandX();

        left.add(shopMenu).left().expand().space(10);
        stage.addActor(left);

        // Tech Menu

        left = new Table();
        left.setFillParent(true);
        left.left().pad(10);

        Table researchMenu = new Table(skin);
        researchMenu.setBackground(skin.getDrawable("window"));
        researchMenu.pad(10);
        researchMenu.setVisible(true);

        Label researchTitle = new Label("Research Technology", skin);
        researchTitle.setAlignment(Align.left);
        researchMenu.add(researchTitle).left().growX().pad(4).row();
        
        left.add(researchMenu).left().expand().space(10);
        stage.addActor(left);

        // Create a fixed-size container for the researchButtonArea
        Table fixedSizeContainer = new Table();
        fixedSizeContainer.setSize(500, 500);
        fixedSizeContainer.setBackground(skin.getDrawable("window"));
        // Use a WidgetGroup to allow free positioning of images
        WidgetGroup researchButtonArea = new WidgetGroup();

        // Find min/max positions to calculate offset
        float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;
        for (Technology tech : Registry.getInstance().getRegistry(Technology.class).values()) {
            minX = Math.min(minX, tech.getPosition().x);
            maxX = Math.max(maxX, tech.getPosition().x);
            minY = Math.min(minY, tech.getPosition().y);
            maxY = Math.max(maxY, tech.getPosition().y);
        }

        // Calculate WidgetGroup size and offset so (0,0) is bottom middle
        float iconSize = 32;
        float spacing = 48;
        float width = (maxX - minX + 1) * spacing;
        float height = (maxY - minY + 1) * spacing;
        researchButtonArea.setSize(width, height);

        float offsetX = width / 2 - iconSize / 2 - (0 - minX) * spacing;
        float offsetY = 0 - minY * spacing;
        offsetX = 0;
        offsetY = 0;

        ButtonGroup<Button> technologyButtonGroup = new ButtonGroup<>();
        technologyButtonGroup.setMaxCheckCount(1);
        technologyButtonGroup.setMinCheckCount(0);

        Table researchDisplayTableContainer = new Table();

        final Table researchDisplayTable = new Table(skin);
        researchDisplayTable.setBackground("window");
        researchDisplayTable.pad(4);
        researchDisplayTable.left();
        researchDisplayTableContainer.add(researchDisplayTable);
        left.add(researchDisplayTableContainer).right();

        // Add technology icons at calculated positions
        for (final Technology tech : Registry.getInstance().getRegistry(Technology.class).values()) {
            Button button = new Button(skin, "toggle");
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (button.isDisabled()) {
                        return;
                    }

                    if (button.equals(technologyButtonGroup.getChecked())) {
                        Entity researchAction = new Entity();
                        gameManager.getEngine().addEntity(researchAction);
                        researchAction.addComponent(new PlayerActionComponent(playerUUID), gameManager.getEngine());
                        researchAction.addComponent(new ResearchActionComponent(tech.getId(), button.isChecked()), gameManager.getEngine());
                        screen.getStream().sendPacket(new EntityPacket(researchAction, true));

                        // Update research display table
                        researchDisplayTable.clearChildren();
                        if (!button.isChecked()) {
                            return;
                        }
                        researchDisplayTable.pad(5);
                        researchDisplayTable.add(tech.getName()).row();

                        Table researchResourceCompletionTable = new Table(skin);

                        Runnable recalculateResearchTable = () -> {
                            researchResourceCompletionTable.clearChildren();
                            int index = 0;
                            int columns = (int) Math.floor(tech.getCost().size() / 2f);
                            columns++;
                            ResearchSystem researchSystem = gameManager.getEngine().getSystem(ResearchSystem.class);
                            ResearchManagementComponent rmc = researchSystem.getPlayerResearchManager(playerUUID.toString());
                            if (!rmc.isResearching()) {
                                return;
                            }
                            HashMap<String, Float> costConsumedMap = rmc.getCostConsumed();
                            for (Entry<String, Float> entry : tech.getCost().entrySet()) {
                                float costConsumed = costConsumedMap.getOrDefault(entry.getKey(), 0f);
                                Table entryTable = getResourceSection(entry.getKey(), costConsumed, entry.getValue(), 0);
                                researchResourceCompletionTable.add(entryTable);
                                if (index % columns == columns - 1) {
                                    researchResourceCompletionTable.row();
                                }
                                index++;
                            }
                        };
                        researchResourceCompletionTable.addAction(Actions.forever(Actions.run(recalculateResearchTable)));
                        researchDisplayTable.add(researchResourceCompletionTable).row();
                    }
                }
            });

            technologyButtonGroup.add(button);
            button.add(new Image(Sprites.getSprite(tech.getIcon()))).center().size(iconSize, iconSize);
            button.setSize(iconSize, iconSize);
            // Position so (0,0) is bottom middle
            float x = offsetX + tech.getPosition().x * spacing;
            float y = offsetY + tech.getPosition().y * spacing;
            button.setPosition(x, y, Align.bottom);
            researchButtonArea.addActor(button);

            Table tooltipTable = new Table(skin);
            tooltipTable.setBackground("window");
            tooltipTable.setColor(Color.GRAY);
            tooltipTable.pad(4);
            
            Label name = new Label(tech.getName(), skin);
            name.setFontScale(.75f);
            name.setAlignment(Align.left);
            tooltipTable.add(name).left().row();
            Label description = new Label(tech.getDescription(), skin);
            description.setFontScale(0.5f);
            description.setAlignment(Align.left);
            tooltipTable.add(description).left().row();
            Table costTable = new Table();

            Runnable recalculateCostTable = () -> {
                costTable.clearChildren();
                int index = 0;
                int columns = (int) Math.floor(tech.getCost().size() / 2f);
                columns++;
                for (Entry<String, Float> entry : tech.getCost().entrySet()) {
                    Table entryTable = getResourceSection(entry.getKey(), entry.getValue(), 0);
                    costTable.add(entryTable);
                    if (index % columns == columns - 1) {
                        costTable.row();
                    }
                    index++;
                }
            };
            costTable.addAction(Actions.forever(Actions.run(recalculateCostTable)));

            tooltipTable.add(costTable);

            Tooltip<Table> tooltip = new Tooltip<>(tooltipTable);
            TooltipManager.getInstance().edgeDistance = 0;
            TooltipManager.getInstance().offsetX = 0;
            TooltipManager.getInstance().offsetY = 0;
            TooltipManager.getInstance().animations = false;
            tooltip.setInstant(true);
            button.addAction(Actions.forever(Actions.run(() -> {
                //Vector2 coordinates = image.screenToLocalCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
                //coordinates.sub(iconSize/2f, iconSize/2f);
                //tooltip.getActor().setPosition(coordinates.x, coordinates.y);
                
                ResearchSystem researchSystem = gameManager.getEngine().getSystem(ResearchSystem.class);
                ResearchManagementComponent rmc = researchSystem.getPlayerResearchManager(playerUUID.toString());
                button.setDisabled(rmc == null || rmc.hasResearched(tech.getId()) || !rmc.prerequisitesMet(tech.getId()));
                researchDisplayTable.setVisible(technologyButtonGroup.getCheckedIndex() != -1);
            })));
            button.addListener(tooltip);
        }

        // Add the WidgetGroup to the fixed-size container
        fixedSizeContainer.add(researchButtonArea).size(width, height);

        // Put the fixed-size container in a ScrollPane
        ScrollPane pane = new ScrollPane(fixedSizeContainer, skin);
        pane.setScrollingDisabled(true, false);
        pane.setOverscroll(false, false);
        pane.setFadeScrollBars(false);

        // Set the preferred size of the ScrollPane to be smaller than the container to enable scrolling
        pane.setForceScroll(false, true); // Enable vertical scrolling
        pane.setScrollbarsOnTop(true);

        researchMenu.add(pane).size(200, 200).center().row();
        Gdx.app.postRunnable(() -> pane.setScrollPercentY(1f));

        // Below

        leftBottom.addAction(Actions.forever(Actions.run(() -> {
            shopMenu.setVisible(shopButton.isChecked());
            researchMenu.setVisible(researchButton.isChecked());
            researchDisplayTableContainer.setVisible(researchButton.isChecked());
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

        // TODO: have the update display update whenever the selected entity is updated in the engine
        final Runnable[] updateDisplay = new Runnable[1];
        updateDisplay[0] = new Runnable() {
            @Override
            public void run() {
                UUID selectedEntityId = selectionSystem.getSelectedId();

                selectionTable.setVisible(true);
                selectionTable.reset();
                if (selectedEntityId == null || gameManager.getEngine().getEntity(selectedEntityId) == null) {
                    return;
                }
                selectionTable.setName(selectedEntityId.toString());

                Entity selectedEntity = gameManager.getEngine().getEntity(selectedEntityId);
                LabelComponent lc = selectedEntity.getComponent(LabelComponent.class);
                TilePositionComponent tpc = selectedEntity.getComponent(TilePositionComponent.class);
                BuildingComponent bc = selectedEntity.getComponent(BuildingComponent.class);
                OwnerComponent oc = selectedEntity.getComponent(OwnerComponent.class);

                // Display name centered
                // Display description underneath in smaller characters
                // Display stats below that, including ownership, production, consumption
                // Display interactions, like recipe selection, destruction buttons, and
                // anything else

                ProducerComponent pc = selectedEntity.getComponent(ProducerComponent.class);
                ConsumerComponent cc = selectedEntity.getComponent(ConsumerComponent.class);
                MovementComponent mc = selectedEntity.getComponent(MovementComponent.class);
                AvailableRecipesComponent arc = selectedEntity.getComponent(AvailableRecipesComponent.class);
                SelectedRecipeComponent src = selectedEntity.getComponent(SelectedRecipeComponent.class);
                ConstructionComponent constc = selectedEntity.getComponent(ConstructionComponent.class);
                HealthComponent hc = selectedEntity.getComponent(HealthComponent.class);

                boolean underConstruction = constc != null && constc.turns > 0;
                boolean producing = gameManager.getEngine().getSystem(ResourceSystem.class).getActiveEntities(playerUUID)
                        .contains(selectedEntity);

                float smallFont = 0.5f;

                Label titleLable = new Label(lc.name, skin);
                Label descriptionLabel = new Label(lc.description, skin);
                Table statsTable = new Table(skin);
                Table interactionTable = new Table(skin);

                descriptionLabel.setFontScale(smallFont);

                if (oc != null && !oc.getUUID().equals(playerUUID)) {
                    // not our building
                    descriptionLabel.getText().append("Owned by: \n"+oc.getUUID().toString());
                } else {
                    if (!underConstruction) {
                        ArrayList<Table> statsSections = new ArrayList<Table>();
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
                                Table resourceSection = getResourceSection(entry.getKey(), entry.getValue(), -1);
                                resourceSection.setColor(producingColor);
                                consumptionTable.add(resourceSection);
                            }
                        }

                        if (pc != null) {
                            Table productionTable = new Table(skin);
                            statsSections.add(productionTable);
                            for (Entry<String, Float> entry : pc.production.entrySet()) {
                                Table resourceSection = getResourceSection(entry.getKey(), entry.getValue(), 1);
                                resourceSection.setColor(producingColor);
                                productionTable.add(resourceSection);
                            }
                        }

                        if (hc != null) {
                            Table healthTable = new Table(skin);
                            statsSections.add(healthTable);
                            Callable<String> healthString = () -> {
                                HealthComponent thc = gameManager.getEngine().getEntity(selectedEntityId).getComponent(HealthComponent.class);
                                return thc.health + " / " + thc.maxHealth + " HP";
                            };
                            Label name;
                            try {
                                name = new Label(healthString.call(), skin);
                                healthTable.add(name);
                                name.addAction(Actions.forever(Actions.run(() -> {
                                    try {
                                        name.setText(healthString.call());
                                    } catch (Exception e) {

                                    }
                                })));
                            } catch (Exception e) {

                            }
                        }
                        
                        if (mc != null) {
                            Table movementTable = new Table(skin);
                            statsSections.add(movementTable);
                            Callable<String> turnString = () -> {
                                MovementComponent tmc = gameManager.getEngine().getEntity(selectedEntityId).getComponent(MovementComponent.class);
                                return tmc.movesRemaining + "/"+ tmc.movesPerTurn + " Turn" + (tmc.movesPerTurn != 1 ? "s" : "");
                            };
                            Label name;
                            try {
                                name = new Label(turnString.call(), skin);
                                movementTable.add(name);
                                name.addAction(Actions.forever(Actions.run(() -> {
                                    try {
                                        name.setText(turnString.call());
                                    } catch (Exception e) {

                                    }
                                })));
                            } catch (Exception e) {

                            }
                        }

                        for (Table table : statsSections) {
                            statsTable.add(table).pad(0, 3, 0, 3).expandY();
                        }

                        // Interactions

                        ArrayList<Table> interactionSections = new ArrayList<>();

                        if (arc != null) {
                            Table availableRecipesTable = new Table(skin);
                            interactionSections.add(availableRecipesTable);

                            SelectBoxStyle style = new SelectBoxStyle(skin.get(SelectBoxStyle.class));

                            Set<String> availableRecipes = new HashSet<>(arc.getAvailableRecipes(selectedEntity, gameManager.getWorld()));
                            availableRecipes.removeIf(id -> {
                                ResearchSystem rs = gameManager.getEngine().getSystem(ResearchSystem.class);
                                if (rs == null) return true;
                                ResearchManagementComponent rmc = rs.getPlayerResearchManager(playerUUID.toString());
                                if (rmc == null) return true;
                                Recipe recipe = Registry.getInstance().getRegistry(Recipe.class).get(id);
                                for (String prereq : recipe.getRequiredTechnologies()) {
                                    if (!rmc.hasResearched(prereq)) {
                                        return true;
                                    }
                                }
                                return false;
                            });

                            SelectBox<String> box = new SelectBox<String>(style);
                            String[] items = new String[availableRecipes.size() + 1];
                            String[] recipeIds = new String[availableRecipes.size()];
                            items[0] = "None";
                            int index = 1;
                            int selectedIndex = 0;
                            for (String recipeId : availableRecipes) {
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
                                    //selectionSystem.deselect();
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
                                        //selectionSystem.deselect();
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

                        descriptionLabel.setText((lc.description + "\nConstruction Finished in " + constc.turns + " turn(s)"));
                        descriptionLabel.addAction(Actions.forever(Actions.run(() -> { 
                            Entity entity = gameManager.getEngine().getEntity(selectedEntityId);
                            if (entity == null) return;
                            ConstructionComponent constc2 = entity.getComponent(ConstructionComponent.class);
                            if (constc2 == null) return;
                            descriptionLabel.setText(lc.description + "\nConstruction Finished in " + constc2.turns + " turn(s)"); 
                        })));
                    }
                    
                }

                int pad = 2;
                selectionTable.add(titleLable).pad(pad).expandX().row();
                selectionTable.add(descriptionLabel).pad(pad).expandX().row();
                selectionTable.add(statsTable).pad(pad).expandX().row();
                selectionTable.add(interactionTable).pad(pad).expandX().row();
            }
        };
        gameManager.getEngine().addEntityChangeListener((e, added) -> {
            updateDisplay[0].run();
        });

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


    public Table getResourceSection(String resourceID, float amount) {
        return getResourceSection(resourceID, amount, 0);
    }

    public Table getResourceSection(String resourceID, float amount, int signValue) {
        return getResourceSection(resourceID, amount, -1, signValue);
    }
    
    /**
     * @param resourceID resource id in registry
     * @param amount amount to display (stored as a label with name "amount")
     * @param outOf optional slash to display along with amount
     * @param signValue < 0 shows negative sign, 0 shows no sign, > 0 shows positive sign
     * @return
     */
    public Table getResourceSection(String resourceID, float amount, float outOf, int signValue) {
        Table table = new Table(skin);
        Resource resource = Registry.getResourceRegistry().get(resourceID);
        Image image = ResourceSystem.getImageDisplay(resource);
        image.setName("image");
        String signString = "";
        if (signValue < 0) {
            signString = "-";
        } else if (signValue > 0) {
            signString = "+";
        }
        String amountString = amount+"";
        String outOfString = outOf+"";
        String finalLabel = signString + amountString + ((outOf >= 0) ? ("/"+outOfString) : "");
        Label label = new Label(finalLabel, skin);
        label.setName("amount");
        table.add(label).padRight(2);
        table.add(image).padRight(2).row();
        return table;
    }

    public TextButton getEndTurnButton() {
        return endTurnButton;
    }

}
