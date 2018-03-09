package com.mygdx.game;

import com.badlogic.gdx.utils.Array;

public class GameMap {

    Node nodes[][];

    private final int width;
    private final int height;

    public GameMap(int width, int height) {
        this.width = width;
        this.height = height;
        nodes = new Node[width][height];

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                nodes[i][j] = new Node(this, i, j);
            }
        }
        updatNearbyNode();
    }

    public Node getNode(int x, int y) {
        Node node = nodes[x][y];
        if (node == null) {
            throw new IllegalStateException("fuck!");
        }
        return node;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void updatNearbyNode() {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                nodes[i][j].updateNearbyNode(this);
            }
        }
    }
}
