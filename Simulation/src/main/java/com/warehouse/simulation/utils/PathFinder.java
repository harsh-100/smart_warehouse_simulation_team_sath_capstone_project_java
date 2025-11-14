package com.warehouse.simulation.utils;

import java.awt.Point;
import java.util.*;
import com.warehouse.simulation.exceptions.ExceptionHandler;

public class PathFinder {

    private final WarehouseGrid grid;

    public PathFinder(WarehouseGrid grid) {
        this.grid = grid;
    }


    public Queue<Point> findPath(Point start, Point end) {

        PriorityQueue<Node> openList = new PriorityQueue<>(Comparator.comparingInt(node -> node.fCost)); // nodes that will be checked
        Set<Node> closedList = new HashSet<>(); // checked nodes

        Node startNode = new Node(start, null, 0, calculateHeuristic(start, end));
        openList.add(startNode);

        while (!openList.isEmpty()) {

            Node currentNode = openList.poll();
            closedList.add(currentNode);

            if (currentNode.point.equals(end)) {
                return reconstructPath(currentNode);
            }

            List<Point> neighbors = getNeighbors(currentNode.point);
            for (Point neighborPoint : neighbors) {

                // allow stepping into the destination cell even if it's occupied
                if (!neighborPoint.equals(end) && !grid.isLocationFree(neighborPoint)) {
                    continue;
                }

                Node neighborNode = new Node(neighborPoint, null, 0, 0);

                if (closedList.contains(neighborNode)) {
                    continue;
                }

                int tentativeGCost = currentNode.gCost + 1;

                if (tentativeGCost < neighborNode.gCost || !openList.contains(neighborNode)) {

                    neighborNode.parent = currentNode;
                    neighborNode.gCost = tentativeGCost;
                    neighborNode.hCost = calculateHeuristic(neighborPoint, end);
                    neighborNode.fCost = neighborNode.gCost + neighborNode.hCost;

                    if (!openList.contains(neighborNode)) {
                        openList.add(neighborNode);
                    }
                }
            }
        }

        return null;
    }


    private int calculateHeuristic(Point a, Point b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    private List<Point> getNeighbors(Point point) {

        List<Point> neighbors = new ArrayList<>();

        if (grid.isWithinBounds(point.x, point.y + 1)) {
            neighbors.add(new Point(point.x, point.y + 1));
        }
        if (grid.isWithinBounds(point.x, point.y - 1)) {
            neighbors.add(new Point(point.x, point.y - 1));
        }
        if (grid.isWithinBounds(point.x + 1, point.y)) {
            neighbors.add(new Point(point.x + 1, point.y));
        }
        if (grid.isWithinBounds(point.x - 1, point.y)) {
            neighbors.add(new Point(point.x - 1, point.y));
        }
        return neighbors;
    }


    private Queue<Point> reconstructPath(Node endNode) {

        LinkedList<Point> path = new LinkedList<>();
        Node currentNode = endNode;

        while (currentNode != null) {
            path.addFirst(currentNode.point);
            currentNode = currentNode.parent;
        }

        return path;
    }
}