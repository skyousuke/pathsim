package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BinaryHeap;
import com.badlogic.gdx.utils.ObjectMap;

public class Pathfinding {

    private Node start;
    private Node goal;

    private NodeRecord[] nodeRecords;
    private BinaryHeap<NodeRecord> frontiers;
    private NodeRecord current;

    private int searchId;

    private Array<Node> path;
    private Array<Node> neighbors;

    private GameMap map;

    private static final int UNVISITED = 0;
    private static final int FRONTIER = 1;
    private static final int VISITED = 2;

    public Pathfinding(GameMap map) {
        path = new Array<Node>();
        neighbors = new Array<Node>(false, 4);
        this.map = map;

        nodeRecords = new NodeRecord[map.getWidth() * map.getHeight()];
        frontiers = new BinaryHeap<NodeRecord>();
    }

    public void start(Node start, Node goal) {
        this.start = start;
        this.goal = goal;

        if (++searchId < 0) searchId = 1;

        path.clear();
        frontiers.clear();

        NodeRecord startRecord = getNodeRecord(start);
        startRecord.node = start;
        startRecord.fromeNode = null;
        startRecord.costSoFar = 0;

        frontiers.add(startRecord, getNodeHeuristic(start, goal));
        start.state = Node.NodeState.FRONTIER;
        start.setSearchId(getSearchId());
    }

    public boolean nextFrontier() {
        if (frontiers.size == 0)
            return false;

        current = frontiers.pop();
        current.node.state = Node.NodeState.CURRENT_FRONTIER;
        current.category = FRONTIER;

        if (current.node.equals(goal)) {
            buildPath();
            return false;
        }

        findNeighbors();
        return true;
    }

    private void findNeighbors() {
        neighbors.clear();
        for (int i = 0; i < current.node.getNearbyNodes().size; i++) {
            Node node = current.node.getNearbyNodes().get(i);
            if (node.state != Node.NodeState.BLOCKED) {
                neighbors.add(node);
                node.drawNeighborFrame = true;
            }
        }
    }

    public boolean nextNeighbor() {
        if (neighbors.size == 0) {
            current.node.state = Node.NodeState.VISITED;
            current.category = VISITED;
            return false;
        }

        Node neighbor = neighbors.pop();
        neighbor.drawNeighborFrame = false;
        int newCost = current.costSoFar + neighbor.cost;
        int nodeHeuristic;

        NodeRecord nodeRecord = getNodeRecord(neighbor);

        if (nodeRecord.category == VISITED) {
            if (nodeRecord.costSoFar <= newCost) return true;
            nodeHeuristic = (int) nodeRecord.getTotalCost() - nodeRecord.costSoFar;
        } else if (nodeRecord.category == FRONTIER) {
            if (nodeRecord.costSoFar <= newCost) return true;
            frontiers.remove(nodeRecord);
            nodeHeuristic = (int) nodeRecord.getTotalCost() - nodeRecord.costSoFar;
        } else {
            nodeHeuristic = getNodeHeuristic(neighbor, goal);
        }

        nodeRecord.costSoFar = newCost;
        nodeRecord.fromeNode = current.node;
        nodeRecord.category = FRONTIER;
        frontiers.add(nodeRecord, (float)(newCost + nodeHeuristic));

        neighbor.setSearchId(getSearchId());
        neighbor.state = Node.NodeState.FRONTIER;
        return true;
    }

    private void buildPath() {
        if (goal == start)
            return;

        while (current.fromeNode != null) {
            path.add(current.node);
            current = nodeRecords[current.fromeNode.getIndex()];
        }
        path.add(start);
        path.reverse();
    }

    public Array<Node> getPath() {
        return path;
    }

    private int getNodeHeuristic(Node start, Node goal) {
        return Math.abs(start.x - goal.x) + Math.abs(start.y - goal.y);
    }

    private NodeRecord getNodeRecord (Node node) {
        int index = node.getIndex();
        NodeRecord nr = nodeRecords[index];
        if (nr != null) {
            if (nr.searchId != searchId) {
                nr.category = UNVISITED;
                nr.searchId = searchId;
            }
            return nr;
        }
        nr = nodeRecords[index] = new NodeRecord();
        nr.node = node;
        nr.searchId = searchId;
        return nr;
    }

    public static class NodeRecord extends BinaryHeap.Node {

        Node node;
        Node fromeNode;
        int costSoFar;
        int category;
        int searchId;

        public NodeRecord() {
            super(0);
        }

        public float getTotalCost() {
            return getValue();
        }
    }

    public NodeRecord[] getNodeRecords() {
        return nodeRecords;
    }

    public int getSearchId() {
        return searchId;
    }
}