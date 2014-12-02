package com.greenyetilab.race;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class RaceGameScreen extends ScreenAdapter {
    private static final float MAX_ACCELEROMETER = 12;
    private final RaceGame mGame;
    private final GameWorld mGameWorld;
    private Batch mBatch;

    private Car mCar;

    private GameRenderer mGameRenderer;

    private Stage mHudStage;
    private ScreenViewport mHudViewport;
    private WidgetGroup mHud;
    private Label mTimeLabel;
    private Label mSpeedLabel;
    private float mTime = 0;

    public RaceGameScreen(RaceGame game, MapInfo mapInfo) {
        mGame = game;
        mBatch = new SpriteBatch();
        mGameWorld = new GameWorld(game, mapInfo);
        mGameRenderer = new GameRenderer(mGameWorld, mBatch);
        setupGameRenderer();
        mCar = mGameWorld.getCar();
        setupHud();
    }

    private void setupGameRenderer() {
        GameRenderer.DebugConfig config = new GameRenderer.DebugConfig();
        Preferences prefs = mGame.getPreferences();
        config.enabled = prefs.getBoolean("debug/enabled", false);
        config.drawTileCorners = prefs.getBoolean("debug/drawTileCorners", false);
        config.drawVelocities = prefs.getBoolean("debug/drawVelocities", false);
        mGameRenderer.setDebugConfig(config);
    }

    void setupHud() {
        mHudViewport = new ScreenViewport();
        mHudStage = new Stage(mHudViewport, mBatch);
        Gdx.input.setInputProcessor(mHudStage);

        Skin skin = mGame.getAssets().skin;
        mHud = new WidgetGroup();

        mTimeLabel = new Label("0:00.0", skin);
        mSpeedLabel = new Label("0", skin);
        mHud.addActor(mTimeLabel);
        mHud.addActor(mSpeedLabel);
        mHud.setHeight(mTimeLabel.getHeight());

        mHudStage.addActor(mHud);
        updateHud();
    }

    @Override
    public void render(float delta) {
        mTime += delta;

        mGameWorld.act(delta);
        mHudStage.act(delta);
        switch (mCar.getState()) {
        case RUNNING:
            break;
        case BROKEN:
            mGame.showGameOverOverlay(mGameWorld.getMapInfo());
            return;
        case FINISHED:
            mGame.showFinishedOverlay(mGameWorld.getMapInfo(), mTime);
            return;
        }

        handleInput();
        updateHud();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        mGameRenderer.render();

        mHudStage.draw();
    }

    private void updateHud() {
        String text = StringUtils.formatRaceTime(mTime);
        mTimeLabel.setText(text);
        mTimeLabel.setPosition(5, 0);

        mSpeedLabel.setText(StringUtils.formatSpeed(mCar.getSpeed()));
        mSpeedLabel.setPosition(mHudViewport.getScreenWidth() - mSpeedLabel.getPrefWidth() - 5, 0);

        mHud.setPosition(0, mHudViewport.getScreenHeight() - mHud.getHeight() - 5);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        mHudViewport.update(width, height, true);
        mGameRenderer.onScreenResized();
    }

    private void handleInput() {
        float direction = 0;
        if (Gdx.input.isPeripheralAvailable(Input.Peripheral.Accelerometer)) {
            float angle = -Gdx.input.getAccelerometerY();
            direction = MathUtils.clamp(angle, -MAX_ACCELEROMETER, MAX_ACCELEROMETER) / MAX_ACCELEROMETER;
        } else {
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                direction = 1;
            } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                direction = -1;
            }
        }
        mCar.setDirection(direction);
        boolean accelerating = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || touchAt(0.5f, 1);
        boolean braking = Gdx.input.isKeyPressed(Input.Keys.SPACE) || touchAt(0, 0.5f);
        mCar.setAccelerating(accelerating);
        mCar.setBraking(braking);
    }

    private boolean touchAt(float startX, float endX) {
        // check if any finger is touching the area between startX and endX
        // startX/endX are given between 0 (left edge of the screen) and 1 (right edge of the screen)
        for (int i = 0; i < 2; i++) {
            float x = Gdx.input.getX() / (float)Gdx.graphics.getWidth();
            if (Gdx.input.isTouched(i) && (x >= startX && x <= endX)) {
                return true;
            }
        }
        return false;
    }
}