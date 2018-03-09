package com.mygdx.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

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
    public int cost;
    public NodeState state;
    public boolean drawNeighborFrame;

    public boolean showCost;
    public boolean showCostSoFar;
    public boolean showHeuristicCost;
    public boolean showAStarCost;
    public boolean showArrow;

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

    public void draw(Pathfinding pathfinding) {
        drawNodeTexture();
        if (searchId == pathfinding.getSearchId()) {
            drawCost(pathfinding);
            drawArrow(pathfinding);
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

    private void drawCost(Pathfinding pathfinding) {
        String text = "";

        if (showCost) {
            text = "" + cost;
        } else if (showCostSoFar) {
            final Pathfinding.NodeRecord nodeRecord = pathfinding.getNodeRecords()[index];
            if (nodeRecord != null) {
                int costSoFar = nodeRecord.costSoFar;
                if (costSoFar != 0) {
                    text = "" + costSoFar;
                }
            }
        } else if (showHeuristicCost) {
            final Pathfinding.NodeRecord nodeRecord = pathfinding.getNodeRecords()[index];
            if (nodeRecord != null) {
                int heuristicCost = (int) nodeRecord.getTotalCost() - nodeRecord.costSoFar;
                if (heuristicCost != 0) {
                    text = "" + heuristicCost;
                }
            }
        } else if (showAStarCost) {
            final Pathfinding.NodeRecord nodeRecord = pathfinding.getNodeRecords()[index];
            if (nodeRecord != null) {
                int aStarCost = (int) nodeRecord.getTotalCost();
                if (aStarCost != 0) {
                    text = "" + aStarCost;
                }
            }
        }

        if (!text.equals("")) {
            game.font.setColor(Color.BLACK);
            GlyphLayout layout = game.font.getCache().setText(text, 0, 0);
            game.font.draw(game.batch, text,
                    x * 34f + 16f - layout.width * 0.5f,
                    y * 34f + layout.height * 0.5f + 16f);
        }
    }

    private void drawArrow(Pathfinding pathfinding) {
        if (!showArrow)
            return;

        final Pathfinding.NodeRecord nodeRecord = pathfinding.getNodeRecords()[index];

        if (nodeRecord != null) {
            Node cameFrom = nodeRecord.fromeNode;
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
