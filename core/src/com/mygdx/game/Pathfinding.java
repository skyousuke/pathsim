package com.mygdx.game;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

public class Pathfinding {

    private Node start;
    private Node goal;

    private Queue<Node> frontiers;
    private ObjectMap<Node, Node> cameFrom;
    private Array<Node> path;
    private Array<Node> neighbors;
    private ObjectMap<Node, Integer> costSoFar;

    private Array<Node> nodes;
    private Node currentFrontier;

    private boolean hasPath;

    public Pathfinding() {
        cameFrom = new ObjectMap<Node, Node>();
        path = new Array<Node>();
        neighbors = new Array<Node>();
        costSoFar = new ObjectMap<Node, Integer>();
    }

    public void start(Node start, final Node goal, Array<Node> nodes) {
        this.start = start;
        this.goal = goal;
        this.nodes = nodes;

        Comparator<Node> comparator = new Comparator<Node>() {
            @Override
            public int compare(Node node1, Node node2) {
                return (node1.findCostSoFar(costSoFar) + node1.findHeuristicCost(goal))
                        - (node2.findCostSoFar(costSoFar) + node2.findHeuristicCost(goal));
            }
        };
        frontiers = new PriorityQueue<Node>(1, comparator);

        costSoFar.put(start, 0);
        frontiers.add(start);
        start.state = Node.NodeState.FRONTIER;
    }

    public void init() {
        hasPath = false;

        costSoFar.clear();
        cameFrom.clear();
    }

    public boolean nextFrontier() {
        if (frontiers.isEmpty())
            return false;

        currentFrontier = frontiers.poll();
        currentFrontier.state = Node.NodeState.CURRENT_FRONTIER;

        if (currentFrontier.equals(goal)) {
            buildPath();
            return false;
        }

        findNeighbors();
        return true;
    }

    public boolean hasPath() {
        return hasPath;
    }

    private void findNeighbors() {
        neighbors.clear();
        for (int i = 0; i < nodes.size; i++) {
            Node node = nodes.get(i);
            if (node == currentFrontier) continue;
            if (node.i == currentFrontier.i - 1 && node.j == currentFrontier.j
                    || node.i == currentFrontier.i + 1 && node.j == currentFrontier.j
                    || node.i == currentFrontier.i && node.j == currentFrontier.j - 1
                    || node.i == currentFrontier.i && node.j == currentFrontier.j + 1) {
                if (node.state != Node.NodeState.BLOCKED) {
                    neighbors.add(node);
                    node.drawNeighborFrame = true;
                }
            }
        }
    }

    public boolean nextNeighbor() {
        if (neighbors.size == 0) {
            currentFrontier.state = Node.NodeState.VISITED;
            return false;
        }

        Node neighbor = neighbors.pop();
        neighbor.drawNeighborFrame = false;
        int newCost = costSoFar.get(currentFrontier) + neighbor.cost;
        if (!costSoFar.containsKey(neighbor) || newCost < costSoFar.get(neighbor)) {
            costSoFar.put(neighbor, newCost);
            frontiers.add(neighbor);
            cameFrom.put(neighbor, currentFrontier);
            neighbor.state = Node.NodeState.FRONTIER;
        }
        return true;
    }

    private void buildPath() {
        path.clear();
        hasPath = true;

        if (goal == start)
            return;

        Node current = goal;
        path.add(current);

        while (!current.equals(start)) {
            current = cameFrom.get(current);
            path.add(current);
        }

        path.reverse();
    }

    public Array<Node> getPath() {
        return path;
    }

    public ObjectMap<Node, Integer> getCostSoFar() {
        return costSoFar;
    }

    public ObjectMap<Node, Node> getCameFrom() {
        return cameFrom;
    }

    public Node getGoal() {
        return goal;
    }
}