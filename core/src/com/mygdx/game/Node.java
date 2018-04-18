package com.mygdx.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public class Node {

    private static MyGdxGame game;

    public enum NodeState {
        DEFAULT,
        FRONTIER,
        CURRENT_FRONTIER,
        VISITED,
        BLOCKED
    }

    public static void setGame(MyGdxGame game) {
        Node.game = game;
    }

    public int x;
    public int y;
    public float cost;
    public NodeState state;
    public boolean drawNeighborFrame;

    public boolean showCost;
    public boolean showCostSoFar;
    public boolean showHeuristicCost;
    public boolean showAStarCost;
    public boolean showArrow;
    private BitmapFontCache cache;
    private String oldCostText = "";

    private final int index;
    private final Array<Node> nearbyNodes;

    private int searchId;

    public Node(GameMap map, int x, int y) {
        this.x = x;
        this.y = y;
        cost = 1;
        index = x * map.getHeight() + y;
        state = NodeState.DEFAULT;
        nearbyNodes = new Array<Node>(4);
    }

    public void updateNearbyNode(GameMap map) {
        nearbyNodes.clear();
        if (x > 0)
            nearbyNodes.add(map.getNode(x - 1, y));
        if (x < map.getWidth() - 1)
            nearbyNodes.add(map.getNode(x + 1, y));
        if (y > 0)
            nearbyNodes.add(map.getNode(x, y - 1));
        if (y < map.getHeight() - 1)
            nearbyNodes.add(map.getNode(x, y + 1));
    }

    public void draw(PathFinder pathFinder) {
        drawNodeTexture();
        if (searchId == pathFinder.getSearchId()) {
            drawArrow(pathFinder);
        }
    }

    private void drawNodeTexture() {
        TextureRegion region = null;
        switch (this.state) {
            case DEFAULT:
                region = game.nodeRegion;
                break;
            case FRONTIER:
                region = game.frontierRegion;
                break;
            case CURRENT_FRONTIER:
                region = game.currentFrontierRegion;
                break;
            case VISITED:
                region = game.visitedRegion;
                break;
            case BLOCKED:
                region = game.blockedRegion;
                break;
        }
        game.batch.draw(region, x * 34f, y * 34f);
        if (drawNeighborFrame)
            game.batch.draw(game.neighborRegion, x * 34f, y * 34f);
    }

    public void drawCost(PathFinder pathFinder) {
        String text = "";

        if (showCost) {
            text = "" + Math.round(cost);
        } else if (searchId == pathFinder.getSearchId()) {
            final PathFinder.NodeRecord nodeRecord = pathFinder.getNodeRecords()[index];
            if (nodeRecord != null) {
                if (showCostSoFar) {
                    int costSoFar = Math.round(nodeRecord.costSoFar);
                    text = "" + costSoFar;
                } else if (showHeuristicCost) {
                    int heuristicCost = Math.round(nodeRecord.getTotalCost() - nodeRecord.costSoFar);
                    text = "" + heuristicCost;
                } else if (showAStarCost) {
                    int aStarCost = Math.round(nodeRecord.getTotalCost());
                    text = "" + aStarCost;
                }
            }
        }

        if (!text.equals("")) {
            if (cache == null) {
                cache = game.font.newFontCache();
                cache.setColor(Color.BLACK);
            }
            if (!text.equals(oldCostText)) {
                GlyphLayout layout = cache.setText(text, x * 34f + 16f, y * 34f + 16f);
                cache.setPosition(-layout.width * 0.5f, layout.height * 0.5f);
                oldCostText = text;
            }
            cache.draw(game.batch);
        }
    }

    private void drawArrow(PathFinder pathFinder) {
        if (!showArrow)
            return;

        final PathFinder.NodeRecord nodeRecord = pathFinder.getNodeRecords()[index];

        if (nodeRecord != null) {
            Node cameFrom = nodeRecord.fromNode;
            if (cameFrom != null) {
                if (cameFrom.x == x && cameFrom.y > y) {
                    game.arrowUp.setPosition(x * 34f, y * 34f);
                    game.arrowUp.draw(game.batch);
                } else if (cameFrom.x == x && cameFrom.y < y) {
                    game.arrowDown.setPosition(x * 34f, y * 34f);
                    game.arrowDown.draw(game.batch);
                } else if (cameFrom.x > x && cameFrom.y == y) {
                    game.arrowRight.setPosition(x * 34f, y * 34f);
                    game.arrowRight.draw(game.batch);
                } else if (cameFrom.x < x && cameFrom.y == y) {
                    game.arrowLeft.setPosition(x * 34f, y * 34f);
                    game.arrowLeft.draw(game.batch);
                }
            }
        }
    }

    public int getIndex() {
        return index;
    }

    public Array<Node> getNearbyNodes() {
        return nearbyNodes;
    }

    public void setSearchId(int searchId) {
        this.searchId = searchId;
    }
}
