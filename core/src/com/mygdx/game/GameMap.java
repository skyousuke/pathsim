package com.mygdx.game;

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
        updateNearbyNode();
    }

    public Node getNode(int x, int y) {
        return nodes[x][y];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void updateNearbyNode() {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                nodes[i][j].updateNearbyNode(this);
            }
        }
    }
}
