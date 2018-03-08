package com.mygdx.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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

    public int i;
    public int j;
    public int cost;
    public NodeState state;
    public boolean drawNeighborFrame;

    public boolean showCost;
    public boolean showCostSoFar;
    public boolean showHeuristicCost;
    public boolean showAStarCost;
    public boolean showArrow;

    public Node(int i, int j) {
        this.i = i;
        this.j = j;
        cost = 1;

        state = NodeState.DEFAULT;
    }

    public int findCostSoFar(ObjectMap<Node, Integer> costSoFar) {
        return costSoFar.get(this, 0);
    }

    public int findHeuristicCost(Node goal) {
        return Math.abs(i - goal.i) + Math.abs(j - goal.j);
    }

    public void draw(Pathfinding pathfinding) {
        drawNodeTexture();
        drawCost(pathfinding);
        drawArrow(pathfinding);
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
        game.batch.draw(region, i * 34f, j * 34f);
        if (drawNeighborFrame)
            game.batch.draw(game.neighborRegion, i * 34f, j * 34f);
    }

    private void drawCost(Pathfinding pathfinding) {
        String text = "";

        if (showCost) {
            text = "" + cost;
        } else if (showCostSoFar) {
            int costSoFar = findCostSoFar(pathfinding.getCostSoFar());
            if (costSoFar != 0) {
                text = "" + costSoFar;
            }
        } else if (showHeuristicCost) {
            if (pathfinding.getCostSoFar().containsKey(this)) {
                int heuristicCost = findHeuristicCost(pathfinding.getGoal());
                text = "" + heuristicCost;
            }
        } else if (showAStarCost && pathfinding.getCostSoFar().containsKey(this)) {
            int aStarCost = findCostSoFar(pathfinding.getCostSoFar()) + findHeuristicCost(pathfinding.getGoal());
            text = "" + aStarCost;
        }

        if (!text.equals("")) {
            game.font.setColor(Color.BLACK);
            GlyphLayout layout = game.font.getCache().setText(text, 0, 0);
            game.font.draw(game.batch, text,
                    i * 34f + 16f - layout.width * 0.5f,
                    j * 34f + layout.height * 0.5f + 16f);
        }
    }

    private void drawArrow(Pathfinding pathfinding) {
        if (!showArrow)
            return;

        final ObjectMap<Node, Node> cameFromMap = pathfinding.getCameFrom();
        if (cameFromMap.containsKey(this)) {
            Node cameFrom = cameFromMap.get(this);
            if (cameFrom.i == i && cameFrom.j > j) {
                game.arrowUp.setPosition(i * 34f, j * 34f);
                game.arrowUp.draw(game.batch);
            }
            else if (cameFrom.i == i && cameFrom.j < j) {
                game.arrowDown.setPosition(i * 34f, j * 34f);
                game.arrowDown.draw(game.batch);
            }
            else if (cameFrom.i > i && cameFrom.j == j) {
                game.arrowRight.setPosition(i * 34f, j * 34f);
                game.arrowRight.draw(game.batch);
            }
            else if (cameFrom.i < i && cameFrom.j == j) {
                game.arrowLeft.setPosition(i * 34f, j * 34f);
                game.arrowLeft.draw(game.batch);
            }
        }
    }
}
