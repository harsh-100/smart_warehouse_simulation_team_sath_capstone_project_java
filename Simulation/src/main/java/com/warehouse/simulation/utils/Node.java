package com.warehouse.simulation.utils;

import java.awt.Point;

public class Node {

    Point point;
    Node parent;
    int gCost;
    int hCost;
    int fCost;

    public Node(Point point, Node parent, int gCost, int hCost) {
        this.point = point;
        this.parent = parent;
        this.gCost = gCost;
        this.hCost = hCost;
        this.fCost = gCost + hCost;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Node node = (Node) obj;
        return point.equals(node.point);
    }

    @Override
    public int hashCode() {
        return point.hashCode();
    }
}