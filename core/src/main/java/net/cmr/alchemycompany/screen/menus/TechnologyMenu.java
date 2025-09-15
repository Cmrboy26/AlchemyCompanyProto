package net.cmr.alchemycompany.screen.menus;

import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import net.cmr.alchemycompany.Sprites;
import net.cmr.alchemycompany.component.actions.PlayerActionComponent;
import net.cmr.alchemycompany.component.actions.ResearchActionComponent;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.game.Registry;
import net.cmr.alchemycompany.game.Technology;
import net.cmr.alchemycompany.helper.MenuHelper;
import net.cmr.alchemycompany.network.packet.EntityPacket;
import net.cmr.alchemycompany.network.packet.EntityPacket.EntityState;
import net.cmr.alchemycompany.system.ResearchSystem;

public class TechnologyMenu extends GameMenu {

    ButtonGroup<TechnologyEntry> techButtonGroup;

    public TechnologyMenu(int alignment, MenuHelper menuHelper) {
        super(alignment, menuHelper, "Technology");
    }

    @Override
    public void construct() {
        WidgetGroup techArea = new WidgetGroup();
        Table techAreaContainer = new Table();
        techAreaContainer.add(techArea).size(200, 800);
        ScrollPane scrollPane = new ScrollPane(techAreaContainer, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setForceScroll(false, true);
        scrollPane.setScrollingDisabled(true, false);
        this.add(scrollPane).size(200, 200);
        this.row();

        techButtonGroup = new ButtonGroup<>();
        techButtonGroup.setMaxCheckCount(1);
        techButtonGroup.setMinCheckCount(0);

        Map<String, Technology> techs = Registry.getInstance().getRegistry(Technology.class);
        for (Technology tech : techs.values()) {
            TechnologyEntry entry = new TechnologyEntry(tech);
            techButtonGroup.add(entry);
            techArea.addActor(entry);
        }

        addContinuousUpdate(() -> {
            ResearchSystem researchSystem = screenHelper.gameManager.getEngine().getSystem(ResearchSystem.class);
            Technology currentResearch = researchSystem.getPlayerResearchManager(screenHelper.playerUUID.toString()).getCurrentResearch();
            techButtonGroup.getButtons().forEach(button -> {
                TechnologyEntry entry = (TechnologyEntry) button;
                boolean hasTechnology = researchSystem.hasTechnology(screenHelper.playerUUID.toString(), entry.tech.getId());
                boolean hasPrerequisites = researchSystem.canResearchTechnology(screenHelper.playerUUID.toString(), entry.tech.getId());
                boolean isResearching = currentResearch != null && currentResearch.getId().equals(entry.tech.getId());
                button.setDisabled(hasTechnology || !hasPrerequisites);
                Color color = Color.WHITE;
                if (hasTechnology) {
                    color = Color.LIGHT_GRAY;
                } else if (!hasPrerequisites) {
                    color = Color.DARK_GRAY;
                } else if (isResearching) {
                    color = Color.YELLOW;
                }
                button.getColor().set(color);
            });
        });

        techArea.pack();
        techArea.setSize(techArea.getPrefWidth(), techArea.getPrefHeight());
        Gdx.app.postRunnable(() -> scrollPane.setScrollPercentY(1f));
    }

    public class TechnologyEntry extends Button {
        private Technology tech;

        public TechnologyEntry(Technology tech) {
            super(skin, "toggle");
            this.tech = tech;
            Image icon = new Image(Sprites.getSprite(tech.getIcon()));
            Vector2 pos = tech.getPosition();

            this.add(icon).size(32).pad(5);
            this.setPosition(pos.x * 48, pos.y * 48);
            this.pack();

            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    /*if (isChecked()) {
                        System.out.println("Selected technology: " + tech.getName());
                        screenHelper.gameManager
                    } else {
                        System.out.println("Deselected technology: " + tech.getName());
                        screenHelper.gameManager.sendResearchAction(tech.getId(), false);
                    }*/
                    Entity researchAction = new Entity();
                    screenHelper.gameManager.getEngine().addEntity(researchAction);
                    researchAction.addComponent(new PlayerActionComponent(screenHelper.playerUUID), screenHelper.gameManager.getEngine());
                    researchAction.addComponent(new ResearchActionComponent(tech.getId(), TechnologyEntry.this.isChecked()), screenHelper.gameManager.getEngine());
                    screenHelper.screen.getStream().sendPacket(new EntityPacket(researchAction, EntityState.ADDED));
                }
            });
        }

        public TechnologyEntry(String techId) {
            this(Registry.getInstance().getRegistry(Technology.class).get(techId));
        }
    }
}
