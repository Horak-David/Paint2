package rasterize;

import model.Line;
import model.Point;
import java.awt.Color;

public abstract class LineRasterizer {
    protected RasterBufferedImage raster;

    protected LineRasterizer(RasterBufferedImage raster) {
        this.raster = raster;
    }

    public abstract void rasterize(int x1, int y1, int x2, int y2, Color c1, Color c2);

    public void rasterize(Point p1, Point p2){
        rasterize(p1.getX(), p1.getY(), p2.getX(), p2.getY(), Color.white, Color.white);
    }

    public void rasterize(Line line) {
        rasterize(line.getX1(), line.getY1(), line.getX2(), line.getY2(), line.c1(), line.c2());
    }
}