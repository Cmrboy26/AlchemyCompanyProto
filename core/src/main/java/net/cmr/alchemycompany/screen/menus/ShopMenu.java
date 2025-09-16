package net.cmr.alchemycompany.screen.menus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Predicate;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

import net.cmr.alchemycompany.Sprites;
import net.cmr.alchemycompany.component.AvailableRecipesComponent;
import net.cmr.alchemycompany.component.BuildingComponent;
import net.cmr.alchemycompany.component.ConstructionComponent;
import net.cmr.alchemycompany.component.LabelComponent;
import net.cmr.alchemycompany.component.PurchaseCostComponent;
import net.cmr.alchemycompany.component.RenderComponent;
import net.cmr.alchemycompany.component.ResearchRequirementComponent;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.entity.BuildingFactory;
import net.cmr.alchemycompany.game.Resources;
import net.cmr.alchemycompany.helper.ScreenHelper;
import net.cmr.alchemycompany.system.ResearchSystem;
import net.cmr.alchemycompany.system.ResourceSystem;
import net.cmr.alchemycompany.world.TilePoint;

public class ShopMenu extends GameMenu {

    private Table categoryTable;
    private Table entryTable;
    private ScrollPane entryScrollPane;

    private ButtonGroup<ShopEntry> entryButtonGroup;
    private ButtonGroup<ShopCategory> categoryButtonGroup;

    public ShopMenu(int align, ScreenHelper screenHelper) {
        super(align, screenHelper, "Shop");
    }

    @Override
    public void construct() {
        entryButtonGroup = new ButtonGroup<>();
        entryButtonGroup.setMaxCheckCount(1);
        entryButtonGroup.setMinCheckCount(0);

        categoryButtonGroup = new ButtonGroup<>();
        categoryButtonGroup.setMaxCheckCount(1);
        categoryButtonGroup.setMinCheckCount(0);

        categoryTable = new Table(skin);
        entryTable = new Table(skin);
        entryScrollPane = new ScrollPane(entryTable, skin);
        entryScrollPane.setFadeScrollBars(false);
        entryScrollPane.setForceScroll(false, true);
        entryScrollPane.setScrollingDisabled(true, false);
        entryScrollPane.pack();

        List<ShopCategory> categoriesList = new ArrayList<>();
        categoriesList.add(new ShopCategory("Can Build", entity -> entity.hasComponent(AvailableRecipesComponent.class), "IRON_ORE_ICON"));
        categoriesList.add(new ShopCategory("Production", entity -> entity.hasComponent(AvailableRecipesComponent.class), "TITANIUM_ICON"));
        ShopCategory miscCategory = new ShopCategory("Miscellaneous", entity -> true, "IRON_ORE_ICON");
        categoriesList.add(miscCategory);

        for (ShopCategory category : categoriesList) {
            categoryTable.add(category).pad(5);
            categoryButtonGroup.add(category);
        }
    
        for (Entry<String, Entity> buildingEntry : BuildingFactory.getRegisteredBuildingEntities().entrySet()) {
            ShopCategory category = categoriesList.stream()
                .filter(entry -> entry.getPredicate().test(buildingEntry.getValue()))
                .findFirst()
                .orElse(miscCategory);
            try {
                ShopEntry shopEntry = new ShopEntry(category, buildingEntry.getValue()) {
                    @Override
                    public String getNotAllowedReason() {
                        // Implement game logic checks here, e.g., technology requirements, resource availability
                        PurchaseCostComponent pcc = shopEntity.getComponent(PurchaseCostComponent.class);
                        ResearchRequirementComponent rrc = shopEntity.getComponent(ResearchRequirementComponent.class);
                        if (rrc != null) {
                            ResearchSystem researchSystem = screenHelper.gameManager.getEngine().getSystem(ResearchSystem.class);
                            for (String techId : rrc.technologiesRequired) {
                                if (!researchSystem.getPlayerResearchManager(screenHelper.playerUUID.toString()).hasResearched(techId)) {
                                    return "Requires technology: " + techId;
                                }
                            }
                        }
                        if (pcc != null) {
                            int existingCount = getExistingCount();
                            ResourceSystem resourceSystem = screenHelper.gameManager.getEngine().getSystem(ResourceSystem.class);
                            for (Entry<String, Float> costEntry : pcc.getResourceCost(existingCount).entrySet()) {
                                String resourceId = costEntry.getKey();
                                Float costAmount = costEntry.getValue();
                                float storageAmount = resourceSystem.getCachedStoredResources(screenHelper.playerUUID).getOrDefault(resourceId, 0f);
                                if (storageAmount < costAmount) {
                                    return "Insufficient resources: " + resourceId;
                                }
                            }
                        }
                        return "";
                    }

                    @Override
                    public int getExistingCount() {
                        BuildingComponent bc = shopEntity.getComponent(BuildingComponent.class);

                        if (bc != null) {
                            return shopEntity.getComponent(PurchaseCostComponent.class).getExistingCount(screenHelper.playerUUID, bc.buildingId, screenHelper.gameManager.getEngine());
                        }
                        return 0;
                    }

                    @Override
                    public void onPlace(TilePoint tilePoint) {
                        String buildingId = buildingEntry.getKey();

                        screenHelper.gameManager.tryPlaceBuilding(screenHelper.playerUUID, buildingId, tilePoint.getX(), tilePoint.getY(), false);
                    }
                };
                entryButtonGroup.add(shopEntry);
                entryTable.add(shopEntry).growX().space(2).row();
            } catch (IllegalArgumentException e) {
                System.err.println("Error creating shop entry for building " + buildingEntry.getKey() + ": " + e.getMessage());
            }
        }

        addContinuousUpdate(() -> {
            if (lastUpdateSecond != (int)System.currentTimeMillis() / 10) {
                lastUpdateSecond = (int)System.currentTimeMillis() / 10;
                entryButtonGroup.getButtons().forEach(entry -> entry.recalculateShopMenu());
            }
        });

        this.add(categoryTable).row();
        this.add(entryScrollPane).height(150).growX().row();
    }

    @Override
    public void onClose() {
        entryButtonGroup.uncheckAll();
    }

    float lastUpdateSecond = 0;

    public ButtonGroup<ShopEntry> getShopEntryGroup() {
        return entryButtonGroup;
    }

    public abstract class ShopEntry extends Button {

        Label nameLabel, warningLabel;
        Image iconImage;
        float elapsedTime = 0;
        ShopCategory category;
        Entity shopEntity;
        Runnable recalculateShopMenu;

        public ShopEntry(ShopCategory category, Entity entity) throws IllegalArgumentException {
            super(skin, "toggle");
            this.category = category;
            this.shopEntity = entity;
            PurchaseCostComponent pcc = entity.getComponent(PurchaseCostComponent.class);
            if (pcc == null) {
                throw new IllegalArgumentException("Entity must have PurchaseCostComponent");
            }
            ConstructionComponent cc = entity.getComponent(ConstructionComponent.class);
            if (cc == null) {
                throw new IllegalArgumentException("Entity must have ConstructionComponent");
            }
            RenderComponent rc = entity.getComponent(RenderComponent.class);
            if (rc == null) {
                throw new IllegalArgumentException("Entity must have RenderComponent");
            }
            LabelComponent lc = entity.getComponent(LabelComponent.class);
            if (lc == null) {
                throw new IllegalArgumentException("Entity must have LabelComponent");
            }

            iconImage = new Image();
            nameLabel = new Label(lc.name, skin);
            warningLabel = new Label(null, skin);
            warningLabel.setWrap(true);
            warningLabel.setFontScale(.6f);

            Table costTable = new Table(skin);
            Table descriptionTable = new Table(skin);
            Runnable recalculateShopMenu = () -> {
                String renderId = rc.renderId;
                if (renderId == null && rc.variants != null && !rc.variants.containsKey("idle")) {
                    renderId = rc.variants.get("idle");
                }
                iconImage.setDrawable(Sprites.getTextureDrawable(rc.renderId, rc.getRenderType(), elapsedTime));

                // Only clear costTable if the cost has changed
                int count = getExistingCount();
                List<Entry<String, Float>> currentCosts = new ArrayList<>(pcc.getResourceCost(count).entrySet());
                Object lastCostsObj = costTable.getUserObject();
                if (!(lastCostsObj instanceof List) || !currentCosts.equals(lastCostsObj)) {
                    costTable.clearChildren();
                    costTable.setUserObject(currentCosts);

                    Image timeImage = new Image(Sprites.getSprite("TIME"));
                    Label timeLabel = new Label(cc.turns + "", skin);

                    costTable.add(timeImage).size(16).pad(1);
                    costTable.add(timeLabel).pad(1).row();
                    for (Entry<String, Float> costEntry : pcc.getResourceCost(count).entrySet()) {
                        Table resourceEntry = Resources.createResourceTable(costEntry.getKey(), 12, costEntry.getValue(), null, null, false);
                        costTable.add(resourceEntry).pad(1).colspan(2).row();
                    }
                }

                setDisabled(!isPurchaseAllowed());
                if (isChecked() && isDisabled()) {
                    setChecked(false);
                }
                String message = getNotAllowedReason();
                if (message.isEmpty() && isChecked()) {
                    message = "Click to place, Shift for multiple";
                }

                warningLabel.setText(message);
                /*descriptionTable.removeActor(warningLabel);
                warningLabel.setText(message);
                if (warningLabel.getText().isEmpty()) {
                    warningLabel.setVisible(false);
                } else {
                    warningLabel.setVisible(true);
                    descriptionTable.add(warningLabel).growX().row();
                }*/
            };
            this.recalculateShopMenu = recalculateShopMenu;
            recalculateShopMenu.run();

            descriptionTable.add(nameLabel).fillX().padBottom(3).row();
            descriptionTable.add(warningLabel).width(125).growX();

            add(iconImage).pad(2).padRight(8).left();
            add(descriptionTable).expandX().left();
            add(costTable).expandX().right();
        }

        public void recalculateShopMenu() {
            recalculateShopMenu.run();
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            elapsedTime += delta;
            setDisabled(!isPurchaseAllowed());
        }

        /**
         * Check if the purchase is allowed based on game logic (e.g., technology requirement met, sufficient resources), as implemented by 
         * @return true if purchase is allowed, false otherwise.
         */
        public final boolean isPurchaseAllowed() {
            return getNotAllowedReason().isEmpty();
        }
        /**
         * Get the reason why the purchase is not allowed, if applicable.
         * @return a string explaining the reason, or an empty string if the purchase is allowed.
         */
        public abstract String getNotAllowedReason();

        /**
         * Get the existing count of this entity type owned by the player.
         * @return the existing count.
         */
        public abstract int getExistingCount();

        /**
         * Action to perform when the player places this entity in the game world.
         */
        public abstract void onPlace(TilePoint tilePoint);
    }

    public class ShopCategory extends Button {
        String name;
        Predicate<Entity> filter;
        String renderId;

        public ShopCategory(String name, Predicate<Entity> filter, String renderId) {
            super(skin, "toggle");
            Image icon = new Image(Sprites.getSprite(renderId));
            this.name = name;
            this.filter = filter;
            this.renderId = renderId;
            add(icon).size(16).pad(1);
            pack();
        }

        public String getName() {
            return name;
        }

        public Predicate<Entity> getPredicate() {
            return filter;
        }

        public String getRenderId() {
            return renderId;
        }
    }

}
