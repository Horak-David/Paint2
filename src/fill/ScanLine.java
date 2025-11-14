package fill;

import model.Edge;
import model.Point;
import model.Polygon;
import rasterize.LineRasterizer;
import rasterize.Raster;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;

public class ScanLine implements Filler {
    private final Raster raster;
    private final LineRasterizer lineRasterizer;
    private final Polygon polygon;
    private final int fillColor;
    private final boolean usePattern;

    public ScanLine(Raster raster, LineRasterizer lineRasterizer, Polygon polygon, int fillColor, boolean usePattern) {
        this.raster = raster;
        this.lineRasterizer = lineRasterizer;
        this.polygon = polygon;
        this.fillColor = fillColor;
        this.usePattern = usePattern;
    }

    @Override
    public void fill() {
        ArrayList<Edge> edges = new ArrayList<>();

        for (int i = 0; i < polygon.points().size(); i++) {
            int indexA = i;
            int indexB = (i + 1) % polygon.points().size();

            Point pA = polygon.getPoint(indexA);
            Point pB = polygon.getPoint(indexB);

            Edge edge = new Edge(pA, pB);

            if (!edge.isHorizontal()) {
                edge.orientate();
                edges.add(edge);
            }
        }

        for (Edge e : edges) {
            lineRasterizer.rasterize(e.getP1().getX(), e.getP1().getY(), e.getP2().getX(), e.getP2().getY(), new Color(fillColor), new Color(fillColor));
        }

        if (edges.isEmpty()) return;

        int yMin = edges.get(0).getP1().getY();
        int yMax = edges.get(0).getP1().getY();

        for (Edge e : edges) {
            yMin = Math.min(yMin, Math.min(e.getP1().getY(), e.getP2().getY()));
            yMax = Math.max(yMax, Math.max(e.getP1().getY(), e.getP2().getY()));
        }

        for (int y = yMin; y <= yMax; y++) {
            ArrayList<Integer> intersections = new ArrayList<>();

            for (Edge drawEdge : edges) {
                if (drawEdge.isIntersecting(y) && y != drawEdge.getP2().getY()) {
                    int x = drawEdge.getIntersection(y);
                    intersections.add(x);
                }
            }

            intersections.sort(Comparator.naturalOrder());

            for (int x = 0; x + 1 < intersections.size(); x += 2) {
                int x1 = intersections.get(x);
                int x2 = intersections.get(x + 1);

                if (usePattern) {
                    int patternSize = 8;
                    int colorA = fillColor;
                    int colorB = Color.BLACK.getRGB();

                    for (int currentX = x1; currentX <= x2; currentX++) {
                        int tileX = currentX / patternSize;
                        int tileY = y / patternSize;

                        int patternColor;
                        if ((tileX + tileY) % 2 == 0) {
                            patternColor = colorA;
                        } else {
                            patternColor = colorB;
                        }

                        raster.setPixel(currentX, y, patternColor);
                    }
                } else {
                    int colorToSet = fillColor;
                    for (int currentX = x1; currentX <= x2; currentX++) {
                        raster.setPixel(currentX, y, colorToSet);
                    }
                }
            }
        }
    }
}