package model;

import java.awt.Color;

public record Line(Point p1, Point p2, Color c1, Color c2) {
    public Line(Point p1, Point p2, Color color) {
        this(p1, p2, color, color);
    }

    public Line(Point p1, Point p2) {
        this(p1, p2, Color.WHITE);
    }

    public int getX1() {
        return p1.getX();
    }

    public int getY1() {
        return p1.getY();
    }

    public int getX2() {
        return p2.getX();
    }

    public int getY2() {
        return p2.getY();
    }
}