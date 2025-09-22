package net.cmr.alchemycompany.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import net.cmr.alchemycompany.AlchemyCompany;
import net.cmr.alchemycompany.GameConnector;
import net.cmr.alchemycompany.Sprites;

public class MainMenuScreen extends ScreenAdapter {
    
    Viewport uiViewport;
    Stage stage;
    String message;

    public MainMenuScreen(String message) {
        this();
        this.message = message;
    }
    public MainMenuScreen() {

    }

    @Override
    public void show() {
        uiViewport = new ExtendViewport(640, 360);
        stage = new Stage(uiViewport, AlchemyCompany.getInstance().batch());

        Skin skin = Sprites.getSkin();
        Table centerTable = new Table();
        centerTable.setFillParent(true);
        centerTable.center();

        Table buttonTable = new Table(skin);
        buttonTable.setBackground("window");
        buttonTable.pad(10);

        TextButton hostGameButton = new TextButton("Host Game", skin);
        hostGameButton.pad(5);
        buttonTable.add(hostGameButton).space(10, 30, 10, 30).growX().row();

        TextButton joinGameButton = new TextButton("Join Game", skin);
        joinGameButton.pad(5);
        buttonTable.add(joinGameButton).space(10, 30, 10, 30).growX().row();

        TextField gameIPField = new TextField("127.0.0.1", skin);
        gameIPField.setMessageText("11265");
        buttonTable.add(gameIPField).space(10, 30, 10, 30).growX().row();
        TextField portField = new TextField("11265", skin);
        portField.setMessageText("11265");
        buttonTable.add(portField).space(10, 30, 10, 30).growX().row();
        
        joinGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String ip = gameIPField.getText();
                Integer port = null;
                try {
                    port = Integer.parseInt(portField.getText());
                    if (port < 1 || port > Short.MAX_VALUE) {
                        throw new NumberFormatException("Please enter a valid port");
                    }
                } catch (NumberFormatException e) {
                    portField.addAction(Actions.sequence(Actions.fadeOut(0.2f), Actions.fadeIn(0.2f)));
                    return;
                }
                GameConnector.joinGame(ip, port);
            }
        });
        hostGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameConnector.hostGame();
            }
        });

        centerTable.add(buttonTable);
        stage.addActor(centerTable);
        Gdx.input.setInputProcessor(stage);

        if (message == null) { return; }
        Window messageWindow = new Window("", skin);
        messageWindow.debugAll();
        messageWindow.getTitleLabel().setAlignment(Align.top);
        messageWindow.pad(5);
        Label messageLabel = new Label(message, skin);
        messageLabel.setFontScale(0.85f);
        messageLabel.setAlignment(Align.center);
        messageLabel.setWrap(true);
        //messageLabel.setFillParent(true);
        messageWindow.add(messageLabel).grow().row();
        messageWindow.setSize(200, 200);
        messageWindow.setPosition(stage.getWidth() / 2, stage.getHeight() / 2, Align.center);
        TextButton closeButton = new TextButton("Continue...", skin);
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                messageWindow.remove();
            }
        });
        messageWindow.add(closeButton).bottom().growX();
        stage.addActor(messageWindow);
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        stage.act(delta);
        //AlchemyCompany.getInstance().batch().begin();
        //AlchemyCompany.getInstance().batch().setProjectionMatrix(uiViewport.getCamera().combined);
        uiViewport.apply(true);
        stage.draw();
        //AlchemyCompany.getInstance().batch().end();
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        uiViewport.update(width, height, true);
    }

}
