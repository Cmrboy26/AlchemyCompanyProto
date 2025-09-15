package net.cmr.alchemycompany.helper;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;

import net.cmr.alchemycompany.GameManager;
import net.cmr.alchemycompany.Sprites;
import net.cmr.alchemycompany.game.Resources;
import net.cmr.alchemycompany.screen.GameScreen;
import net.cmr.alchemycompany.screen.menus.GameMenu;
import net.cmr.alchemycompany.screen.menus.ShopMenu;
import net.cmr.alchemycompany.screen.menus.TechnologyMenu;
import net.cmr.alchemycompany.system.ResourceSystem;
import net.cmr.alchemycompany.system.TurnSystem;

public class MenuHelper extends ScreenHelper {

    Stage stage;
    //ButtonGroup<Button> shopGroup = new ButtonGroup<>();
    ButtonGroup<TextButton> menuSelectorGroup = new ButtonGroup<>();
    TextButton endTurnButton = null;
    private Map<Class<? extends GameMenu>, GameMenu> menus = new HashMap<>();

    public MenuHelper(GameScreen screen, GameManager gameManager, UUID playerUUID, Stage stage) {
        super(screen, gameManager, playerUUID);
        this.stage = stage;
        this.menus = new HashMap<>();
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
            button.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    menuSelectorTable.setVisible(false);
                    menuInstance.getParent().addAction(Actions.sequence(
                        Actions.visible(true),
                        Actions.moveToAligned(0, stage.getHeight() / 2, Align.left, 0.25f, Interpolation.sineOut)
                    ));
                    System.out.println(menuInstance.getParent().getX() + " " + menuInstance.getParent().getY());
                    menuInstance.toFront();
                    return true;
                }
            });
            button.pack();
            menuSelectorTable.add(button).width(70).pad(2).row();
            menuSelectorGroup.add(button);
            
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
            hudTable.clearChildren();
            hudTable.pack();
            
            turnLabel.setFontScale(0.75f);
            hudTable.pad(4);

            hudTable.add(turnLabel).growX().left().space(8);
            populateResourceTable(hudTable, false, (resId -> {
                return resId.equals("GOLD") || resId.equals("SCIENCE");
            }));
        })));

        Table miscResourcesTable = new Table(skin);
        miscResourcesTable.setBackground(skin.getDrawable("window"));
        miscResourcesTable.pad(4);
        topMenu.add(miscResourcesTable).right().space(8);
        populateResourceTable(miscResourcesTable, true, (resId -> {
            return !resId.equals("GOLD") && !resId.equals("SCIENCE");
        }));

    }

    private void populateResourceTable(Table resourceTable, boolean vertical, Predicate<String> filter) {
        Map<String, Float> resourceMap = gameManager.getEngine().getSystem(ResourceSystem.class).getCachedStoredResources(playerUUID);
        Map<String, Float> productionMap = gameManager.getEngine().getSystem(ResourceSystem.class).getCachedResourcePerSecond(playerUUID);
        Map<String, Float> totalStorageMap = gameManager.getEngine().getSystem(ResourceSystem.class).getCachedTotalStorageCapacity(playerUUID);
        for (Entry<String, Float> resourceEntry : resourceMap.entrySet()) {
            if (filter != null && !filter.test(resourceEntry.getKey())) continue;
            /*Table resourceSection = getResourceSection(8, resourceEntry.getKey(), resourceEntry.getValue(),
                    productionMap.containsKey(resourceEntry.getKey()) ? productionMap.get(resourceEntry.getKey()) : -1, 
                    productionMap.containsKey(resourceEntry.getKey()) ? 1 : 0);*/
            Table resourceSection = Resources.createResourceTable(resourceEntry.getKey(), 8, resourceEntry.getValue(), totalStorageMap.get(resourceEntry.getKey()), productionMap.get(resourceEntry.getKey()), false);
            resourceTable.add(resourceSection).right().fillX();
            if (vertical) resourceTable.row();
        }
        
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

    public <T extends GameMenu> T getMenu(Class<T> menuClass) {
        return menuClass.cast(menus.get(menuClass));
    }

    /*public Table getResourceSection(String resourceID, float amount) {
        return getResourceSection(resourceID, amount, 0);
    }

    public Table getResourceSection(String resourceID, float amount, int signValue) {
        return getResourceSection(12, resourceID, amount, -1, signValue);
    }*/
    
    /**
     * @param resourceID resource id in registry
     * @param amount amount to display (stored as a label with name "amount")
     * @param outOf optional slash to display along with amount
     * @param signValue < 0 shows negative sign, 0 shows no sign, > 0 shows positive sign
     * @return
     */
    /*public Table getResourceSection(float imageSize, String resourceID, float amount, float outOf, int signValue) {
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
        table.add(image).padRight(2).size(imageSize).row();
        return table;
    }*/

    public TextButton getEndTurnButton() {
        return endTurnButton;
    }

}
