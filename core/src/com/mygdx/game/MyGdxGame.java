package com.mygdx.game;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.skyousuke.gdxutils.CoordUtils;
import com.github.skyousuke.gdxutils.skin.Skins;

public class MyGdxGame extends InputAdapter implements ApplicationListener {

    private TextureAtlas atlas;

    public TextureRegion nodeRegion;
    public TextureRegion frontierRegion;
    public TextureRegion currentFrontierRegion;
    public TextureRegion neighborRegion;
    public TextureRegion visitedRegion;
    public TextureRegion blockedRegion;
    private TextureRegion startRegion;
    private TextureRegion goalRegion;

    public Sprite arrowRight;
    public Sprite arrowUp;
    public Sprite arrowLeft;
    public Sprite arrowDown;

    private Sprite pathHorizontal;
    private Sprite pathVertical;

    public SpriteBatch batch;

    private Stage stage;
    private Viewport viewport;

    private Skin skin;
    public BitmapFont font;

    private Array<Node> nodes = new Array<Node>();
    private Node startNode;
    private Node goalNode;
    private Pathfinding pathfinding;

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

    private Stack firstStepLabel;
    private Stack secondStepLabel;
    private Stack thirdStepLabel;
    private Stack fourthStepLabel;

    private Pool<Image> selectionImagePool = new Pool<Image>() {
        @Override
        protected Image newObject() {
            Image image = new Image(skin.get(List.ListStyle.class).selection);
            image.setFillParent(true);
            return image;
        }
    };
    private CheckBox pathCheckbox;
    private TextButton runButton;

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

        skin = Skins.DEFAULT_THAI.createSkin();

        font = skin.getFont("default");

        skin.get(TextButton.TextButtonStyle.class).up =
                ((NinePatchDrawable) skin.get(TextButton.ButtonStyle.class).up).tint(Color.GRAY);

        disabledButtonStyle = new TextButton.TextButtonStyle(skin.get(TextButton.TextButtonStyle.class));
        disabledButtonStyle.up =
                ((NinePatchDrawable) skin.get(TextButton.ButtonStyle.class).up).tint(Color.DARK_GRAY);
        disabledButtonStyle.fontColor = Color.GRAY;

        final float screenWidth = Gdx.graphics.getWidth();
        final float screenHeight = Gdx.graphics.getHeight();

        batch = new SpriteBatch();
        stage = new Stage(new FitViewport(screenWidth, screenHeight));
        viewport = new FitViewport(screenWidth, screenHeight);
        viewport.getCamera().position.set(screenWidth / 2, screenHeight / 2, 0);

        Gdx.input.setInputProcessor(new InputMultiplexer(stage, this));
        Node.setGame(this);


        Table menu = new Table();

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
                for (int i = 0; i < nodes.size; i++) {
                    nodes.get(i).showCost = costCheckBox.isChecked();
                }
            }
        });


        final CheckBox costSoFarCheckBox = new CheckBox("แสดง Cost รวมนับจากจุด Start", skin);
        costSoFarCheckBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                for (int i = 0; i < nodes.size; i++) {
                    nodes.get(i).showCostSoFar = costSoFarCheckBox.isChecked();
                }
            }
        });

        final CheckBox heuristicCostCheckBox = new CheckBox("แสดง Heuristic Cost", skin);
        heuristicCostCheckBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                for (int i = 0; i < nodes.size; i++) {
                    nodes.get(i).showHeuristicCost = heuristicCostCheckBox.isChecked();
                }
            }
        });

        final CheckBox aStarCostCheckBox = new CheckBox("แสดง A* Cost", skin);
        aStarCostCheckBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                for (int i = 0; i < nodes.size; i++) {
                    nodes.get(i).showAStarCost = aStarCostCheckBox.isChecked();
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
                for (int i = 0; i < nodes.size; i++) {
                    nodes.get(i).showArrow = arrowCheckbox.isChecked();
                }
            }
        });

        pathCheckbox = new CheckBox("แสดงเส้นทาง", skin);

        menu.align(Align.bottomLeft);
        menu.row().left();
        menu.add(costCheckBox).colspan(3);
        menu.row().left().padTop(5f);
        menu.add(costSoFarCheckBox).colspan(3);
        menu.row().left().padTop(5f);
        menu.add(heuristicCostCheckBox).colspan(3);
        menu.row().left().padTop(5f);
        menu.add(aStarCostCheckBox).colspan(3);
        menu.row().left().padTop(5f);
        menu.add(arrowCheckbox);
        menu.add(pathCheckbox).colspan(2);
        menu.row().left().padTop(10f);
        menu.add(resetButton).size(120, 30).padRight(5f);
        menu.add(clearBlockedButton).size(150, 30).colspan(2);
        menu.row().left().padTop(10f);
        menu.add(runButton).size(120, 30).padRight(5f);
        menu.add(new Label("ความเร็ว", skin)).padRight(5f);
        menu.add(speedSlider).size(100, 30);

        menu.row().left().padTop(10f);
        menu.add(new Label("Step การทำงาน", skin)).colspan(3);

        firstStepLabel = new Stack(new Label("1. ให้จุด start เป็น frontier", skin));
        secondStepLabel = new Stack(new Label("2. หา neighbor รอบๆ frontier", skin));
        thirdStepLabel = new Stack(new Label("3. บันทึกลูกศร และให้ neighbor เป็น frontier", skin));
        fourthStepLabel = new Stack(new Label("4. เจอ goal แล้วลากเส้นตามลูกศรจาก goal มา start", skin));

        menu.row().left().padTop(5f);
        menu.add(firstStepLabel).colspan(3);
        menu.row().left().padTop(5f);
        menu.add(secondStepLabel).colspan(3);
        menu.row().left().padTop(5f);
        menu.add(thirdStepLabel).colspan(3);
        menu.row().left().padTop(5f);
        menu.add(fourthStepLabel).colspan(3);

        menu.row().left().padTop(10f);
        menu.add(nextStepButton).size(120, 30).colspan(3);

        menu.row().left().padTop(15f);
        menu.add(new Label("วิธีการใช้งาน", skin)).colspan(3);
        menu.row().left().padTop(5f);
        menu.add(new Label("- คลิกที่ Grid สีขาวเพื่อเพิ่ม/ลบสิ่งกีดขวาง", skin)).colspan(3);
        menu.row().left().padTop(5f);
        menu.add(new Label("- ลากรูป Satoshi เพื่อปรับจุด Start", skin)).colspan(3);
        menu.row().left().padTop(5f);
        menu.add(new Label("- ลากรูป Pikachu เพื่อปรับจุด Goal", skin)).colspan(3);
        menu.row().left().padTop(5f);
        menu.add(new Label("- กดค้นหาเส้นทาง เพื่อดู Animation การทำงาน", skin)).colspan(3);

        menu.setPosition(700, 30);

        stage.addActor(menu);

        pathfinding = new Pathfinding();

        generateNode();

        startNode = nodes.get(98);
        goalNode = nodes.get(188);

        arrowCheckbox.setChecked(true);
        pathCheckbox.setChecked(true);
    }

    private void reset() {
        for (int i = 0; i < nodes.size; i++) {
            final Node node = nodes.get(i);
            if (node.state != Node.NodeState.BLOCKED)
                node.state = Node.NodeState.DEFAULT;
            node.drawNeighborFrame = false;
        }
        pathfinding.init();
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

    private void generateNode() {
        for (int i = 1; i < 20; i++) {
            for (int j = 1; j < 16; j++) {
                nodes.add(new Node(i, j));
            }
        }
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
        for (int i = 0; i < nodes.size; i++) {
            nodes.get(i).draw(pathfinding);
        }
        batch.draw(startRegion, startNode.i * 34f, startNode.j * 34f);
        batch.draw(goalRegion, goalNode.i * 34f, goalNode.j * 34f);
        if (pathCheckbox.isChecked())
            drawPath();
        batch.end();

        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        stage.getViewport().update(width, height);
    }

    @Override
    public void dispose() {
        batch.dispose();
        atlas.dispose();
        skin.dispose();
        stage.dispose();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    private void nextStep() {
        switch (state) {
            case START:
                pathfinding.start(startNode, goalNode, nodes);
                state = GameState.SELECT_FRONTIER;
                setSelectedLabel(firstStepLabel, true);
                break;
            case SELECT_FRONTIER:
                setSelectedLabel(firstStepLabel, false);
                setSelectedLabel(thirdStepLabel, false);
                boolean finished = !pathfinding.nextFrontier();
                if (finished) {
                    state = GameState.FINISH;
                    nextStepButton.setText("จบการทำงาน");
                    nextStepButton.setTouchable(Touchable.disabled);
                    nextStepButton.setStyle(disabledButtonStyle);
                    setSelectedLabel(fourthStepLabel, true);

                    runningMode = false;
                    runButton.setTouchable(Touchable.disabled);
                    runButton.setStyle(disabledButtonStyle);
                    runButton.setText("เจอเส้นทางแล้ว");
                } else {
                    state = GameState.SELECT_NEIGHBOR;
                    setSelectedLabel(secondStepLabel, true);
                }
                break;
            case SELECT_NEIGHBOR:
                boolean hasNeighbor;
                do hasNeighbor = pathfinding.nextNeighbor();
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
                if (node.state == Node.NodeState.DEFAULT) {
                    node.state = Node.NodeState.BLOCKED;
                    addBlockedMode = true;
                } else if (node.state == Node.NodeState.BLOCKED) {
                    node.state = Node.NodeState.DEFAULT;
                    removeBlockedMode = true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean touchDragged(int touchX, int touchY, int pointer) {
        if (runningMode) return false;
        Node node = findNodeFromTouch(touchX, touchY);
        if (node != null) {
            if (addBlockedMode && (node != startNode && node != goalNode) && node.state == Node.NodeState.DEFAULT)
                node.state = Node.NodeState.BLOCKED;

            if (removeBlockedMode && node.state == Node.NodeState.BLOCKED)
                node.state = Node.NodeState.DEFAULT;

            if (setStartMode && node != goalNode && node.state != Node.NodeState.BLOCKED)
                startNode = node;

            if (setGoalMode && node != startNode && node.state != Node.NodeState.BLOCKED)
                goalNode = node;

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
        for (int i = 0; i < nodes.size; i++) {
            Node node = nodes.get(i);
            if (node.i == x && node.j == y)
                return node;
        }
        return null;
    }

    private void clearBlocked() {
        for (int i = 0; i < nodes.size; i++) {
            Node node = nodes.get(i);
            if (node.state == Node.NodeState.BLOCKED)
                node.state = Node.NodeState.DEFAULT;
        }
    }

    private void drawPath() {
        if (pathfinding.hasPath()) {
            Array<Node> path = pathfinding.getPath();
            for (int i = 0; i + 1 < path.size; i++) {
                Node firstNode = path.get(i);
                Node secondNode = path.get(i + 1);

                if (firstNode.i == secondNode.i && firstNode.j > secondNode.j) {
                    pathVertical.setPosition(firstNode.i * 34f, firstNode.j * 34f - 17);
                    pathVertical.draw(batch);
                }
                if (firstNode.i == secondNode.i && firstNode.j < secondNode.j) {
                    pathVertical.setPosition(firstNode.i * 34f, firstNode.j * 34f + 17);
                    pathVertical.draw(batch);

                } else if (firstNode.i < secondNode.i && firstNode.j == secondNode.j) {
                    pathHorizontal.setPosition(firstNode.i * 34f + 17, firstNode.j * 34f);
                    pathHorizontal.draw(batch);

                } else if (firstNode.i > secondNode.i && firstNode.j == secondNode.j) {
                    pathHorizontal.setPosition(firstNode.i * 34f - 17, firstNode.j * 34f);
                    pathHorizontal.draw(batch);
                }
            }
        }
    }

    private void setSelectedLabel(Stack label, boolean selected) {
        if (selected) {
            if (label.getChildren().size == 1) {
                label.addActorAt(0, selectionImagePool.obtain());
            }
        } else {
            if (label.getChildren().size > 1) {
                Image image = (Image) label.getChildren().get(0);
                label.removeActor(image);
                selectionImagePool.free(image);
            }
        }
    }

    private float speedLevelToStepTime(int speedLevel) {
        if (speedLevel == 0)
            return 100;
        return 100f / (60 * speedLevel);
    }
}
