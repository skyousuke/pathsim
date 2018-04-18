package com.mygdx.game;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.StringBuilder;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.skyousuke.gdxutils.CoordUtils;
import com.github.skyousuke.gdxutils.skin.Skins;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.*;

public class MyGdxGame extends InputAdapter implements ApplicationListener {

    private TextureAtlas atlas;

    TextureRegion nodeRegion;
    TextureRegion frontierRegion;
    TextureRegion currentFrontierRegion;
    TextureRegion neighborRegion;
    TextureRegion visitedRegion;
    TextureRegion blockedRegion;
    private TextureRegion startRegion;
    private TextureRegion goalRegion;

    Sprite arrowRight;
    Sprite arrowUp;
    Sprite arrowLeft;
    Sprite arrowDown;

    private Sprite pathHorizontal;
    private Sprite pathVertical;

    SpriteBatch batch;

    private Stage stage;
    private Viewport viewport;

    private Skin skin;
    BitmapFont font;

    private GameMap map;
    private Node startNode;
    private Node goalNode;
    private PathFinder pathFinder;

    private boolean removeBlockedMode;
    private boolean addBlockedMode;
    private boolean setStartMode;
    private boolean setGoalMode;
    private boolean runningMode;

    private TextButton nextStepButton;

    private GameState state = GameState.START;
    private float timeAccumulator = 0f;
    private float stepTime = 0.10f;

    private TextButton.TextButtonStyle disabledButtonStyle;

    private Label firstStepLabel;
    private Label secondStepLabel;
    private Label thirdStepLabel;
    private Label fourthStepLabel;

    private CheckBox pathCheckbox;
    private TextButton runButton;

    private TextField heuristicField;
    private Window heuristicWindow;

    private float screenWidth;
    private float screenHeight;

    private Cursor mouseCursor;
    private Cursor handCursor;

    public enum GameState {
        START,
        SELECT_FRONTIER,
        SELECT_NEIGHBOR,
        FINISH
    }

    @Override
    public void create() {
        atlas = new TextureAtlas("image.atlas");
        nodeRegion = atlas.findRegion("node");
        frontierRegion = atlas.findRegion("frontier");
        currentFrontierRegion = atlas.findRegion("currentFrontier");
        neighborRegion = atlas.findRegion("neighbor");
        visitedRegion = atlas.findRegion("visited");
        blockedRegion = atlas.findRegion("blocked");
        startRegion = atlas.findRegion("start");
        goalRegion = atlas.findRegion("goal");

        TextureRegion arrowRegion = atlas.findRegion("arrow");
        TextureRegion pathRegion = atlas.findRegion("path");

        arrowRight = new Sprite(arrowRegion);
        arrowUp = new Sprite(arrowRegion);
        arrowLeft = new Sprite(arrowRegion);
        arrowDown = new Sprite(arrowRegion);

        arrowUp.rotate(90);
        arrowLeft.rotate(180);
        arrowDown.rotate(-90);

        pathHorizontal = new Sprite(pathRegion);
        pathVertical = new Sprite(pathRegion);
        pathVertical.rotate(90);

        if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
            mouseCursor = Gdx.graphics.newCursor(new Pixmap(Gdx.files.internal("cursor1.png")), 0, 0);
            handCursor = Gdx.graphics.newCursor(new Pixmap(Gdx.files.internal("cursor2.png")), 0, 0);
            Gdx.graphics.setCursor(mouseCursor);
        }

        skin = Skins.DEFAULT_THAI.createSkin();
        skin.getRegion("white").getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        font = skin.getFont("default");

        skin.get(TextButton.TextButtonStyle.class).up =
                ((NinePatchDrawable) skin.get(TextButton.ButtonStyle.class).up).tint(Color.GRAY);

        disabledButtonStyle = new TextButton.TextButtonStyle(skin.get(TextButton.TextButtonStyle.class));
        disabledButtonStyle.up =
                ((NinePatchDrawable) skin.get(TextButton.ButtonStyle.class).up).tint(Color.DARK_GRAY);
        disabledButtonStyle.fontColor = Color.GRAY;

        screenWidth = Gdx.graphics.getWidth();
        screenHeight = Gdx.graphics.getHeight();

        batch = new SpriteBatch();
        stage = new Stage(new FitViewport(screenWidth, screenHeight));
        viewport = new FitViewport(screenWidth, screenHeight);
        viewport.getCamera().position.set(screenWidth / 2 - 30, screenHeight / 2 - 70, 0);

        Gdx.input.setInputProcessor(new InputMultiplexer(stage, this));
        Node.setGame(this);

        map = new GameMap(19, 15);

        Table rightUi = new Table();
        Table bottomUi = new Table();

        TextButton clearBlockedButton = new TextButton("ลบสี่งกีดขวางทั้งหมด", skin);
        clearBlockedButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                clearBlocked();
            }
        });

        TextButton resetButton = new TextButton("รีเซต", skin);
        resetButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                reset();
            }
        });

        runButton = new TextButton("เริ่มหาเส้นทาง", skin);
        runButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                runningMode = true;
                runButton.setText("กำลังทำงาน");
                runButton.setTouchable(Touchable.disabled);
                runButton.setStyle(disabledButtonStyle);
            }
        });

        final Slider speedSlider = new Slider(0, 100, 1, false, skin);
        speedSlider.setValue((int) (100f / (60 * stepTime)));
        speedSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                stepTime = speedLevelToStepTime((int) speedSlider.getValue());
            }
        });

        final CheckBox costCheckBox = new CheckBox("แสดง Cost ของช่อง", skin);
        costCheckBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                for (int i = 0; i < map.getWidth(); i++) {
                    for (int j = 0; j < map.getHeight(); j++) {
                        map.getNode(i, j).showCost = costCheckBox.isChecked();
                    }
                }
            }
        });


        final CheckBox costSoFarCheckBox = new CheckBox("แสดง Cost รวมนับจากจุด Start", skin);
        costSoFarCheckBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                for (int i = 0; i < map.getWidth(); i++) {
                    for (int j = 0; j < map.getHeight(); j++) {
                        map.getNode(i, j).showCostSoFar = costSoFarCheckBox.isChecked();
                    }
                }
            }
        });

        final CheckBox heuristicCostCheckBox = new CheckBox("แสดง Heuristic Cost", skin);
        heuristicCostCheckBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                for (int i = 0; i < map.getWidth(); i++) {
                    for (int j = 0; j < map.getHeight(); j++) {
                        map.getNode(i, j).showHeuristicCost = heuristicCostCheckBox.isChecked();
                    }
                }
            }
        });

        final CheckBox aStarCostCheckBox = new CheckBox("แสดง A* Cost", skin);
        aStarCostCheckBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                for (int i = 0; i < map.getWidth(); i++) {
                    for (int j = 0; j < map.getHeight(); j++) {
                        map.getNode(i, j).showAStarCost = aStarCostCheckBox.isChecked();
                    }
                }
            }
        });

        ButtonGroup<CheckBox> checkBoxGroup = new ButtonGroup<CheckBox>();
        checkBoxGroup.setMinCheckCount(0);
        checkBoxGroup.add(costCheckBox,
                costSoFarCheckBox,
                heuristicCostCheckBox,
                aStarCostCheckBox);

        nextStepButton = new TextButton("Step ถัดไป", skin);
        nextStepButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                nextStep();
            }
        });

        final CheckBox arrowCheckbox = new CheckBox("แสดงลูกศร", skin);
        arrowCheckbox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                for (int i = 0; i < map.getWidth(); i++) {
                    for (int j = 0; j < map.getHeight(); j++) {
                        map.getNode(i, j).showArrow = arrowCheckbox.isChecked();
                    }
                }
            }
        });

        pathCheckbox = new CheckBox("แสดงเส้นทาง", skin);

        rightUi.align(Align.bottomLeft);
        rightUi.row().left();
        rightUi.add(costCheckBox);
        rightUi.add(costSoFarCheckBox).spaceLeft(10f);
        rightUi.row().left().padTop(5f);
        rightUi.add(heuristicCostCheckBox);
        rightUi.add(aStarCostCheckBox).spaceLeft(10f);

        rightUi.row().left().padTop(15f);
        rightUi.add(newLabelWithBackgroundColor("Step การทำงาน", skin, Color.NAVY)).colspan(2);

        firstStepLabel = newOwnStyleLabel("1. ให้จุด start เป็น frontier", skin);
        secondStepLabel = newOwnStyleLabel("2. หา neighbor รอบๆ frontier", skin);
        thirdStepLabel = newOwnStyleLabel("3. บันทึกลูกศร และให้ neighbor เป็น frontier", skin);
        fourthStepLabel = newOwnStyleLabel("4. เมื่อเจอ goal ให้ทำเส้นทางตามลูกศรจาก goal มา start", skin);

        rightUi.row().left().padTop(5f);
        rightUi.add(firstStepLabel).colspan(2);
        rightUi.row().left().padTop(5f);
        rightUi.add(secondStepLabel).colspan(2);
        rightUi.row().left().padTop(5f);
        rightUi.add(thirdStepLabel).colspan(2);
        rightUi.row().left().padTop(5f);
        rightUi.add(fourthStepLabel).colspan(2);

        rightUi.row().left().padTop(10f);
        rightUi.add(nextStepButton).size(120, 30).colspan(2);

        rightUi.row().left().padTop(15f);
        rightUi.add(newLabelWithBackgroundColor("วิธีการใช้งาน", skin, Color.FOREST)).colspan(2);
        rightUi.row().left().padTop(5f);
        rightUi.add(new Label("- คลิกที่ Grid สีขาวเพื่อเพิ่ม/ลบสิ่งกีดขวาง", skin)).colspan(2);
        rightUi.row().left().padTop(5f);
        rightUi.add(new Label("- ลากรูป Satoshi เพื่อปรับจุด Start", skin)).colspan(2);
        rightUi.row().left().padTop(5f);
        rightUi.add(new Label("- ลากรูป Pikachu เพื่อปรับจุด Goal", skin)).colspan(2);
        rightUi.row().left().padTop(5f);
        rightUi.add(new Label("- กดเริ่มหาเส้นทาง เพื่อดู Animation การทำงาน", skin)).colspan(2);

        pathFinder = new PathFinder(map);
        Label heuristicLabel = new Label(pathFinder.getHeuristic(), skin);
        heuristicLabel.setEllipsis(true);

        heuristicField = new TextField(pathFinder.getHeuristic(), skin);
        heuristicField.getStyle().background = ((TextureRegionDrawable) skin.getDrawable("white")).tint(Color.BROWN);

        heuristicWindow = createHeuristicWindow(heuristicLabel);
        Button changeHeuristicButton = new TextButton("เปลี่ยน Heuristic", skin);
        changeHeuristicButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                heuristicWindow.addAction(Actions.sequence(visible(true), fadeIn(0.4f)));
                stage.setKeyboardFocus(heuristicField);
                heuristicField.setCursorPosition(heuristicField.getText().length());
                heuristicField.setSelection(0, heuristicField.getText().length());
            }
        });

        rightUi.row().left().padTop(15f);
        rightUi.add(newLabelWithBackgroundColor("Heuristic Function", skin, Color.FIREBRICK)).colspan(2);
        rightUi.row().left().padTop(10f);
        rightUi.add(heuristicLabel).colspan(2).width(340);
        rightUi.row().left().padTop(10f);
        rightUi.add(changeHeuristicButton).size(120, 30);
        rightUi.pack();
        rightUi.setPosition(700, 560, Align.topLeft);

        bottomUi.add(clearBlockedButton).size(150, 30).padRight(5f);
        bottomUi.add(resetButton).size(70, 30).padRight(5f);
        bottomUi.add(runButton).size(120, 30).padRight(10f);
        bottomUi.add(new Label("ความเร็ว", skin)).padRight(5f);
        bottomUi.add(speedSlider).size(120, 30).padRight(10f);
        bottomUi.add(arrowCheckbox).padRight(15f);
        bottomUi.add(pathCheckbox).padRight(15f);
        bottomUi.pack();

        bottomUi.setPosition(30, 20);

        stage.addActor(rightUi);
        stage.addActor(bottomUi);
        stage.addActor(heuristicWindow);

        startNode = map.getNode(5, 10);
        goalNode = map.getNode(15, 10);

        arrowCheckbox.setChecked(true);
        pathCheckbox.setChecked(true);
    }

    private Label newOwnStyleLabel(String text, Skin skin) {
        return new Label(text, new Label.LabelStyle(skin.get(Label.LabelStyle.class)));
    }

    private void reset() {
        for (int i = 0; i < map.getWidth(); i++) {
            for (int j = 0; j < map.getHeight(); j++) {
                final Node node = map.getNode(i, j);
                if (node.state != Node.NodeState.BLOCKED)
                    node.state = Node.NodeState.DEFAULT;
                node.drawNeighborFrame = false;
                node.setSearchId(-1);
            }
        }
        map.updateNearbyNode();

        pathFinder.getPath().clear();

        state = GameState.START;
        nextStepButton.setTouchable(Touchable.enabled);
        nextStepButton.setText("Step ถัดไป");
        nextStepButton.setStyle(skin.get(TextButton.TextButtonStyle.class));

        setSelectedLabel(firstStepLabel, false);
        setSelectedLabel(secondStepLabel, false);
        setSelectedLabel(thirdStepLabel, false);
        setSelectedLabel(fourthStepLabel, false);

        runningMode = false;
        runButton.setText("เริ่มหาเส้นทาง");
        runButton.setTouchable(Touchable.enabled);
        runButton.setStyle(skin.get(TextButton.TextButtonStyle.class));
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.getCamera().update();
        batch.setProjectionMatrix(viewport.getCamera().combined);

        if (runningMode) {
            timeAccumulator += Gdx.graphics.getRawDeltaTime();
            while (timeAccumulator > stepTime) {
                timeAccumulator -= stepTime;
                nextStep();
            }
        }
        stage.act();

        batch.begin();
        for (int i = 0; i < map.getWidth(); i++) {
            for (int j = 0; j < map.getHeight(); j++) {
                map.getNode(i, j).draw(pathFinder);
            }
        }
        if (pathCheckbox.isChecked())
            drawPath();
        batch.draw(startRegion, startNode.x * 34f, startNode.y * 34f);
        batch.draw(goalRegion, goalNode.x * 34f, goalNode.y * 34f);
        for (int i = 0; i < map.getWidth(); i++) {
            for (int j = 0; j < map.getHeight(); j++) {
                map.getNode(i, j).drawCost(pathFinder);
            }
        }
        batch.end();

        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        stage.getViewport().update(width, height);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        batch.dispose();
        atlas.dispose();
        skin.dispose();
        stage.dispose();
        if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
            mouseCursor.dispose();
            handCursor.dispose();
        }
    }

    private void nextStep() {
        switch (state) {
            case START:
                pathFinder.start(startNode, goalNode);
                state = GameState.SELECT_FRONTIER;
                setSelectedLabel(firstStepLabel, true);
                break;
            case SELECT_FRONTIER:
                setSelectedLabel(firstStepLabel, false);
                setSelectedLabel(thirdStepLabel, false);
                boolean finished = !pathFinder.nextFrontier();
                if (finished) {
                    state = GameState.FINISH;
                    nextStepButton.setText("ทำงานจบแล้ว");
                    nextStepButton.setTouchable(Touchable.disabled);
                    nextStepButton.setStyle(disabledButtonStyle);
                    setSelectedLabel(fourthStepLabel, true);
                    runningMode = false;
                    runButton.setText("เจอเส้นทางแล้ว");
                } else {
                    state = GameState.SELECT_NEIGHBOR;
                    setSelectedLabel(secondStepLabel, true);
                }
                break;
            case SELECT_NEIGHBOR:
                boolean hasNeighbor;
                do hasNeighbor = pathFinder.nextNeighbor();
                while (hasNeighbor);
                state = GameState.SELECT_FRONTIER;
                setSelectedLabel(secondStepLabel, false);
                setSelectedLabel(thirdStepLabel, true);
                break;
        }
    }

    @Override
    public boolean touchDown(int touchX, int touchY, int pointer, int button) {
        if (runningMode) return false;
        Node node = findNodeFromTouch(touchX, touchY);
        if (node == null)
            return false;

        if (button == 0) {
            if (node == startNode) {
                setStartMode = true;
            } else if (node == goalNode) {
                setGoalMode = true;
            } else {
                if (node.state != Node.NodeState.BLOCKED) {
                    node.state = Node.NodeState.BLOCKED;
                    addBlockedMode = true;
                } else {
                    node.state = Node.NodeState.DEFAULT;
                    removeBlockedMode = true;
                }
                map.updateNearbyNode();
            }
        }

        return false;
    }

    @Override
    public boolean touchDragged(int touchX, int touchY, int pointer) {
        if (runningMode) return false;
        Node node = findNodeFromTouch(touchX, touchY);
        if (node == null)
            return false;

        if (node.state != Node.NodeState.BLOCKED) {
            if (setStartMode) {
                startNode = node;
            } else if (setGoalMode) {
                goalNode = node;
            } else if (addBlockedMode && node != startNode && node != goalNode) {
                node.state = Node.NodeState.BLOCKED;
                map.updateNearbyNode();
            }
        } else if (removeBlockedMode) {
            node.state = Node.NodeState.DEFAULT;
            map.updateNearbyNode();
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        addBlockedMode = false;
        removeBlockedMode = false;
        setStartMode = false;
        setGoalMode = false;
        return false;
    }

    private Node findNodeFromTouch(int touchX, int touchY) {
        Vector2 position = CoordUtils.touchToWorld(touchX, touchY, viewport.getCamera(), viewport);
        int x = (int) (position.x / 34f);
        int y = (int) (position.y / 34f);
        Pools.free(position);
        for (int i = 0; i < map.getWidth(); i++) {
            for (int j = 0; j < map.getHeight(); j++) {
                Node node = map.getNode(i, j);
                if (node.x == x && node.y == y)
                    return node;
            }
        }
        return null;
    }

    private void clearBlocked() {
        for (int i = 0; i < map.getWidth(); i++) {
            for (int j = 0; j < map.getHeight(); j++) {
                Node node = map.getNode(i, j);
                if (node.state == Node.NodeState.BLOCKED)
                    node.state = Node.NodeState.DEFAULT;
            }
        }
        map.updateNearbyNode();
    }

    private void drawPath() {
        if (pathFinder.getPath().size > 0) {
            Array<Node> path = pathFinder.getPath();
            for (int i = 0; i + 1 < path.size; i++) {
                Node firstNode = path.get(i);
                Node secondNode = path.get(i + 1);

                if (firstNode.x == secondNode.x && firstNode.y > secondNode.y) {
                    pathVertical.setPosition(firstNode.x * 34f, firstNode.y * 34f - 17);
                    pathVertical.draw(batch);
                }
                if (firstNode.x == secondNode.x && firstNode.y < secondNode.y) {
                    pathVertical.setPosition(firstNode.x * 34f, firstNode.y * 34f + 17);
                    pathVertical.draw(batch);

                } else if (firstNode.x < secondNode.x && firstNode.y == secondNode.y) {
                    pathHorizontal.setPosition(firstNode.x * 34f + 17, firstNode.y * 34f);
                    pathHorizontal.draw(batch);

                } else if (firstNode.x > secondNode.x && firstNode.y == secondNode.y) {
                    pathHorizontal.setPosition(firstNode.x * 34f - 17, firstNode.y * 34f);
                    pathHorizontal.draw(batch);
                }
            }
        }
    }

    private void setSelectedLabel(Label label, boolean selected) {
        if (selected) {
            label.getStyle().background = skin.get(List.ListStyle.class).selection;
        } else {
            label.getStyle().background = null;
        }
    }

    private float speedLevelToStepTime(int speedLevel) {
        if (speedLevel == 0)
            return 0.5f;
        return 100f / (60 * speedLevel);
    }

    private Window createHeuristicWindow(final Label heuristicLabel) {
        final Window window = new Window("กำหนด Heuristic Function", skin);

        final Label currentHeuristic = new Label(pathFinder.getHeuristic(), skin);
        currentHeuristic.setEllipsis(true);

        TextButton okButton = new TextButton("ตกลง", skin);
        okButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                boolean success = pathFinder.setHeuristic(heuristicField.getText());

                if (success) {
                    currentHeuristic.setText(pathFinder.getHeuristic());
                    heuristicLabel.setText(pathFinder.getHeuristic());
                    window.addAction(Actions.sequence(fadeOut(0.4f), Actions.visible(false)));
                } else {
                    Dialog dialog = new Dialog("มีปัญหาของการกำหนด Heuristic Function", skin);
                    dialog.text("Heuristic Function ไม่ถูกต้อง!" +
                            "\nกรุณาตรวจสอบให้แน่ใจว่ากรอกถูกต้อง" +
                            "\nหรือไม่มีตัวหารเป็น 0 (ตัวแปรมีโอกาสเป็น 0 ได้)");
                    dialog.getCells().first().padTop(20);
                    dialog.setPosition(screenWidth / 2, screenHeight / 2, Align.center);
                    Button button = new TextButton("เข้าใจแล้ว!", skin);
                    dialog.button(button).getButtonTable().getCell(button).size(80, 30).padTop(10);
                    dialog.pad(20);
                    dialog.pack();
                    dialog.show(stage);
                }
            }
        });

        TextButton cancelButton = new TextButton("ยกเลิก", skin);
        cancelButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                heuristicField.setText(pathFinder.getHeuristic());
                window.addAction(Actions.sequence(fadeOut(0.4f), Actions.visible(false)));
            }
        });

        Table textAddingButtonTable = new Table();
        textAddingButtonTable.left();
        textAddingButtonTable.add(new Label("ตัวแปร:", skin)).width(40);
        textAddingButtonTable.add(newTextAddingButton("nodeX", skin)).padLeft(10).height(30);
        textAddingButtonTable.add(newTextAddingButton("nodeY", skin)).padLeft(5).height(30);
        textAddingButtonTable.add(newTextAddingButton("goalX", skin)).padLeft(5).height(30);
        textAddingButtonTable.add(newTextAddingButton("goalY", skin)).padLeft(5).height(30);
        textAddingButtonTable.add(new Label("ตัวดำเนินการ:", skin)).width(80).padLeft(10);
        textAddingButtonTable.add(newTextAddingButton("+", skin)).padLeft(10).height(30);
        textAddingButtonTable.add(newTextAddingButton("-", skin)).padLeft(5).height(30);
        textAddingButtonTable.add(newTextAddingButton("*", skin)).padLeft(5).height(30);
        textAddingButtonTable.add(newTextAddingButton("/", skin)).padLeft(5).height(30);
        textAddingButtonTable.add(newTextAddingButton("%", skin)).padLeft(5).height(30);
        textAddingButtonTable.add(newTextAddingButton("^", skin)).padLeft(5).height(30);

        Table functionButtonTable = new Table();
        functionButtonTable.left();
        functionButtonTable.add(new Label("ฟังก์ชัน:", skin)).width(40);
        functionButtonTable.add(newFunctionButton("ABS", skin)).padLeft(10).height(30);
        functionButtonTable.add(newFunctionButton("SQRT", skin)).padLeft(5).height(30);
        functionButtonTable.add(newFunctionButton("LOG", skin)).padLeft(5).height(30);
        functionButtonTable.add(newFunctionButton("LOG10", skin)).padLeft(5).height(30);
        functionButtonTable.add(newFunctionButton("MIN", skin)).padLeft(5).height(30);
        functionButtonTable.add(newFunctionButton("MAX", skin)).padLeft(5).height(30);
        functionButtonTable.add(newFunctionButton("ROUND", skin)).padLeft(5).height(30);
        functionButtonTable.add(newFunctionButton("FLOOR", skin)).padLeft(5).height(30);
        functionButtonTable.add(newFunctionButton("CEILING", skin)).padLeft(5).height(30);
        functionButtonTable.add(newFunctionButton("RAD", skin)).padLeft(5).height(30);
        functionButtonTable.add(newFunctionButton("DEG", skin)).padLeft(5).height(30);

        Table functionButtonTable2 = new Table();
        functionButtonTable2.left();
        functionButtonTable2.add(newFunctionButton("SIN", skin)).padLeft(50).height(30);
        functionButtonTable2.add(newFunctionButton("COS", skin)).padLeft(5).height(30);
        functionButtonTable2.add(newFunctionButton("TAN", skin)).padLeft(5).height(30);
        functionButtonTable2.add(newFunctionButton("CSC", skin)).padLeft(5).height(30);
        functionButtonTable2.add(newFunctionButton("SEC", skin)).padLeft(5).height(30);
        functionButtonTable2.add(newFunctionButton("COT", skin)).padLeft(5).height(30);
        functionButtonTable2.add(newFunctionButton("ASIN", skin)).padLeft(5).height(30);
        functionButtonTable2.add(newFunctionButton("ACOS", skin)).padLeft(5).height(30);
        functionButtonTable2.add(newFunctionButton("ATAN", skin)).padLeft(5).height(30);
        functionButtonTable2.add(newFunctionButton("ACOT", skin)).padLeft(5).height(30);
        functionButtonTable2.add(newFunctionButton("ATAN2", skin)).padLeft(5).height(30);

        Table functionButtonTable3 = new Table();
        functionButtonTable3.left();
        functionButtonTable3.add(newFunctionButton("SINH", skin)).padLeft(50).height(30);
        functionButtonTable3.add(newFunctionButton("COSH", skin)).padLeft(5).height(30);
        functionButtonTable3.add(newFunctionButton("TANH", skin)).padLeft(5).height(30);
        functionButtonTable3.add(newFunctionButton("CSCH", skin)).padLeft(5).height(30);
        functionButtonTable3.add(newFunctionButton("SECH", skin)).padLeft(5).height(30);
        functionButtonTable3.add(newFunctionButton("ASINH", skin)).padLeft(5).height(30);
        functionButtonTable3.add(newFunctionButton("ACOSH", skin)).padLeft(5).height(30);
        functionButtonTable3.add(newFunctionButton("ATANH", skin)).padLeft(5).height(30);
        functionButtonTable3.add(newFunctionButton("RANDOM", skin)).padLeft(5).height(30);

        final Label functionLink = new Label("ดูรายละเอียดของฟังก์ชันที่นี่", skin);
        functionLink.setColor(Color.CYAN);
        functionLink.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.net.openURI("https://github.com/uklimaschewski/EvalEx#supported-functions");
            }
        });
        functionLink.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (Gdx.app.getType() == Application.ApplicationType.WebGL) {
                    Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Hand);
                } else {
                    Gdx.graphics.setCursor(handCursor);
                }
                functionLink.setColor(Color.YELLOW);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (Gdx.app.getType() == Application.ApplicationType.WebGL) {
                    Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
                } else {
                    Gdx.graphics.setCursor(mouseCursor);
                }
                functionLink.setColor(Color.CYAN);
            }
        });

        window.pad(20);
        window.top().left();
        window.row().left().padTop(10);
        window.add(new Label("Heuristic Function ปัจจุบัน:", skin)).colspan(3);
        window.row().left().padTop(5);
        Cell currentHeuristicCell = window.add(currentHeuristic).colspan(3);
        window.row().left().padTop(10);
        window.add(new Label("Heuristic Function ใหม่:", skin)).colspan(3);
        window.row().left();
        window.add(heuristicField).expandX().fillX();
        window.add(okButton).size(55, 30).padLeft(10);
        window.add(cancelButton).size(55, 30).padLeft(5);
        window.row().left().padTop(10);
        window.add(textAddingButtonTable).colspan(3);
        window.row().left().padTop(10);
        window.add(functionButtonTable).colspan(3);
        window.row().left().padTop(10);
        window.add(functionButtonTable2).colspan(3);
        window.row().left().padTop(10);
        window.add(functionButtonTable3).colspan(3);
        window.row().left().padTop(10);
        window.add(functionLink).colspan(3).padLeft(50);
        window.pack();
        currentHeuristicCell.width(functionButtonTable.getWidth());
        window.setPosition(screenWidth / 2, screenHeight / 2, Align.center);
        window.addAction(alpha(0));
        window.setVisible(false);
        return window;
    }

    private Button newTextAddingButton(final String variableName, Skin skin) {
        TextButton variableButton = new TextButton(' ' + variableName + ' ', skin);
        variableButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                final String selection = heuristicField.getSelection();
                if (!selection.equals("")) {
                    setSelectionText(heuristicField, variableName);
                } else {
                    insertText(heuristicField, variableName);
                }
            }
        });
        return variableButton;
    }

    private Button newFunctionButton(final String functionName, Skin skin) {
        TextButton functionButton = new TextButton(' ' + functionName + ' ', skin);
        functionButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                final String selection = heuristicField.getSelection();
                if (!selection.equals("")) {
                    setSelectionText(heuristicField, functionName + '(' + selection + ')');
                } else {
                    insertText(heuristicField, functionName + "()");
                }
            }
        });
        return functionButton;
    }

    private Label newLabelWithBackgroundColor(String text, Skin skin, Color backgroundColor) {
        TextureRegionDrawable drawable = (TextureRegionDrawable) skin.getDrawable("white");
        Label.LabelStyle style = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        style.background = drawable.tint(backgroundColor);
        return new Label(text, style);
    }

    private void setSelectionText(TextField textField, String newText) {
        int selectionStart = textField.getSelectionStart();
        int cursorPosition = textField.getCursorPosition();
        int minIndex = Math.min(selectionStart, cursorPosition);
        int maxIndex = Math.max(selectionStart, cursorPosition);
        StringBuilder sb = new StringBuilder(textField.getText());
        sb.delete(minIndex, maxIndex);
        sb.insert(minIndex, newText);
        textField.setText(sb.toString());
        textField.setCursorPosition(minIndex + newText.length());
    }

    private void insertText(TextField textField, String text) {
        int cursorPosition = textField.getCursorPosition();
        StringBuilder sb = new StringBuilder(textField.getText());
        sb.insert(cursorPosition, text);
        textField.setText(sb.toString());
        textField.setCursorPosition(cursorPosition + text.length());
    }
}
