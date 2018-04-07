package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BinaryHeap;
import com.udojava.evalex.Expression;

import java.math.BigDecimal;

public class PathFinder {

    private static final String DEFAULT_HEURISTIC = "ABS(nodeX - goalX) + ABS(nodeY - goalY)";

    private Node start;
    private Node goal;
    private String heuristic = DEFAULT_HEURISTIC;

    private NodeRecord[] nodeRecords;
    private BinaryHeap<NodeRecord> frontiers;
    private NodeRecord current;

    private int searchId;

    private Array<Node> path;
    private Array<Node> neighbors;

    private static final int UNVISITED = 0;
    private static final int FRONTIER = 1;
    private static final int VISITED = 2;

    public PathFinder(GameMap map) {
        path = new Array<Node>();
        neighbors = new Array<Node>(false, 4);

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
        startRecord.fromNode = null;
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
        float newCost = current.costSoFar + neighbor.cost;
        float nodeHeuristic;

        NodeRecord nodeRecord = getNodeRecord(neighbor);

        if (nodeRecord.category == VISITED) {
            if (nodeRecord.costSoFar <= newCost) return true;
            nodeHeuristic = nodeRecord.getTotalCost() - nodeRecord.costSoFar;
        } else if (nodeRecord.category == FRONTIER) {
            if (nodeRecord.costSoFar <= newCost) return true;
            frontiers.remove(nodeRecord);
            nodeHeuristic = nodeRecord.getTotalCost() - nodeRecord.costSoFar;
        } else {
            nodeHeuristic = getNodeHeuristic(neighbor, goal);
        }

        nodeRecord.costSoFar = newCost;
        nodeRecord.fromNode = current.node;
        nodeRecord.category = FRONTIER;
        frontiers.add(nodeRecord, newCost + nodeHeuristic);

        neighbor.setSearchId(getSearchId());
        neighbor.state = Node.NodeState.FRONTIER;
        return true;
    }

    private void buildPath() {
        if (goal == start)
            return;

        while (current.fromNode != null) {
            path.add(current.node);
            current = nodeRecords[current.fromNode.getIndex()];
        }
        path.add(start);
        path.reverse();
    }

    public Array<Node> getPath() {
        return path;
    }

    private float getNodeHeuristic(Node node, Node goal) {
        BigDecimal nodeX = new BigDecimal(node.x);
        BigDecimal nodeY = new BigDecimal(node.y);
        BigDecimal goalX = new BigDecimal(goal.x);
        BigDecimal goalY = new BigDecimal(goal.y);

        Expression expression = new Expression(heuristic)
                .with("nodeX", nodeX)
                .with("goalX", goalX)
                .with("nodeY", nodeY)
                .with("goalY", goalY);
        return expression.eval().floatValue();
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
        Node fromNode;
        float costSoFar;
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

    public boolean setHeuristic(String heuristic) {
        this.heuristic = heuristic;
        Expression expression = new Expression(heuristic)
                .with("nodeX", new BigDecimal(0))
                .with("goalX", new BigDecimal(0))
                .with("nodeY", new BigDecimal(0))
                .with("goalY", new BigDecimal(0));
        boolean hasException = false;
        try {
            expression.eval();
        } catch (Expression.ExpressionException e) {
            hasException = true;
        } catch (ArithmeticException e) {
            hasException = true;
        }
        if (hasException) {
            this.heuristic = DEFAULT_HEURISTIC;
            return false;
        }
        return true;
    }

    public String getHeuristic() {
        return heuristic;
    }
}