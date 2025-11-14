package model;

import java.awt.Color;

public class Edge {
    private Point p1;
    private Point p2;
    private final Color c1;
    private final Color c2;

    public Edge(Point p1, Point p2, Color color) {
        this(p1, p2, color, color);
    }

    public Edge(Point p1, Point p2, Color c1, Color c2) {
        this.p1 = p1;
        this.p2 = p2;
        this.c1 = c1;
        this.c2 = c2;
    }

    public Edge(Point p1, Point p2) {
        this(p1, p2, Color.WHITE);
    }

    public Point getP1() { return p1; }
    public Point getP2() { return p2; }

    public int getX1() { return p1.getX(); }
    public int getY1() { return p1.getY(); }
    public int getX2() { return p2.getX(); }
    public int getY2() { return p2.getY(); }

    public Color getC1() { return c1; }
    public Color getC2() { return c2; }

    public boolean isHorizontal(){
        return this.p1.getY() == this.p2.getY();
    }

    public void orientate(){
        if(this.p1.getY() > this.getY2()){
            Point temp = this.p1;
            this.p1 = this.p2;
            this.p2 = temp;
        }
    }

    public boolean isIntersecting(int y) {
        return p1.getY() <= y && y < p2.getY();
    }

    public int getIntersection(int y){
        int dy = p2.getY() - p1.getY();
        if(dy == 0) return p1.getX();

        float t = (float)(y - p1.getY()) / dy;
        int x = Math.round(p1.getX() + t * (p2.getX() - p1.getX()));

        return x;
    }
}