package clip;

import model.Point;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Clipper {
    public static class Edge {
        Point p1;
        Point p2;

        public Edge(Point p1, Point p2) {
            this.p1 = p1;
            this.p2 = p2;
        }

        public boolean inside(Point p) {
            Point v1 = new Point(this.p2.getX() - this.p1.getX(), this.p2.getY() - this.p1.getY());
            Point n1 = new Point(-v1.getY(), v1.getX());
            Point v2 = new Point(p.getX() - this.p1.getX(), p.getY() - this.p1.getY());
            return (n1.getX() * v2.getX() + n1.getY() * v2.getY() < 0.0D);
        }

        public boolean inside2(Point p) {
            return ((this.p2.getY() - this.p1.getY()) * p.getX() - (this.p2.getX() - this.p1.getX()) * p.getY() + this.p2.getX() * this.p1.getY() - this.p2.getY() * this.p1.getX() > 0.0D);
        }

        public double distance(Point p) {
            return Math.abs(((this.p2.getY() - this.p1.getY()) * p.getX() - (this.p2.getX() - this.p1.getX()) * p.getY() + this.p2.getX() * this.p1.getY() - this.p2.getY() * this.p1.getX()) / Math.sqrt((this.p2.getY() - this.p1.getY()) * (this.p2.getY() - this.p1.getY()) + (this.p2.getX() - this.p1.getX()) * (this.p2.getX() - this.p1.getX())));
        }

        public Point intersection(Point v1, Point v2) {
            double numX = (v1.getX() * v2.getY() - v1.getY() * v2.getX()) * (this.p1.getX() - this.p2.getX()) - (this.p1.getX() * this.p2.getY() - this.p1.getY() * this.p2.getX()) * (v1.getX() - v2.getX());

            double numY = (v1.getX() * v2.getY() - v1.getY() * v2.getX()) * (this.p1.getY() - this.p2.getY()) - (this.p1.getX() * this.p2.getY() - this.p1.getY() * this.p2.getX()) * (v1.getY() - v2.getY());

            double den = (v1.getX() - v2.getX()) * (this.p1.getY() - this.p2.getY()) - (this.p1.getX() - this.p2.getX()) * (v1.getY() - v2.getY());

            if (Math.abs(den) < 1e-9) return null;

            double px = numX / den;
            double py = numY / den;

            return new Point((int) Math.round(px), (int) Math.round(py));
        }
    }

    public List<Point> clip(List<Point> points, List<Point> clipPoints) {
        if (clipPoints.size() < 2) return points;

        List<Point> newPoints = points;
        Point p1 = clipPoints.get(clipPoints.size() - 1);

        for (Point p2 : clipPoints) {
            newPoints = clipEdge(newPoints, new Edge(p1, p2));
            p1 = p2;
        }

        return newPoints;
    }

    private List<Point> clipEdge(List<Point> points, Edge e) {
        if (points.size() < 2) return points;

        List<Point> out = new ArrayList<>();
        Point v1 = points.get(points.size() - 1);

        for (Point v2 : points) {
            if (e.inside(v2)) {
                if (!e.inside(v1)) {
                    Point intersection = e.intersection(v1, v2);
                    if (intersection != null) out.add(intersection);
                }
                out.add(v2);
            } else if (e.inside(v1)) {
                Point intersection = e.intersection(v1, v2);
                if (intersection != null) out.add(intersection);
            }
            v1 = v2;
        }

        return out;
    }
}