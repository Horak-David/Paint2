package model;

import java.util.ArrayList;

public record Polygon(ArrayList<Point> points) {
    public Polygon() {
        this(new ArrayList<>());
    }

    public Polygon(ArrayList<Point> points) {
        this.points = new ArrayList<>(points);
    }

    public void addPoint(Point p) {
        this.points.add(p);
    }

    public Point getPoint(int index) {
        return points.get(index);
    }

    public Point getLast() {
        return points.getLast();
    }
}