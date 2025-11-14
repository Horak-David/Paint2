package rasterize;

import java.awt.Color;

// Algoritmus rasterizace: triviální metoda (y = k * x + q)
// Postup algoritmu:
// 1 - Spočítá sklon úsečky k = dy / dx (nebo naopak, když je úsečka strmá)
// 2 - Určí, po které ose bude krokovat buď po x (vodorovné úsečky), nebo po y (svislé/strmé úsečky)
// 3 - Každým krokem zvětší souřadnici o 1 a druhou dopočítá podle rovnice (y = k * x + q nebo x = k * y + q)
// 4 - Zaokrouhlí souřadnici na druhé ose na nejbližší pixel a ten vykreslí

public class LineRasterizerTrivial extends LineRasterizer {
    public LineRasterizerTrivial(RasterBufferedImage raster) {
        super(raster);
    }

    public void rasterize(int x1, int y1, int x2, int y2, Color c1, Color c2) {
        if (x1 == x2 && y1 == y2) {
            setPixelSafe(x1, y1, c1);
            return;
        }

        int dx = x2 - x1;
        int dy = y2 - y1;

        if (Math.abs(dy) > Math.abs(dx)) {
            if (y1 > y2) {
                int tmp = x1;
                x1 = x2;
                x2 = tmp;
                tmp = y1;
                y1 = y2;
                y2 = tmp;
                Color tc = c1;
                c1 = c2;
                c2 = tc;
            }
            float k = dx / (float) dy;
            int steps = Math.abs(y2 - y1);
            for (int i = 0; i <= steps; i++) {
                int y = y1 + i;
                int x = Math.round(k * (y - y1) + x1);
                Color color = lerpColor(c1, c2, (float) i / steps);
                setPixelSafe(x, y, color);
            }
        } else {
            if (x1 > x2) {
                int tmp = x1;
                x1 = x2;
                x2 = tmp;
                tmp = y1;
                y1 = y2;
                y2 = tmp;
                Color tc = c1;
                c1 = c2;
                c2 = tc;
            }
            float k = dy / (float) dx;
            float q = y1 - k * x1;
            int steps = Math.abs(x2 - x1);
            for (int i = 0; i <= steps; i++) {
                int x = x1 + i;
                int y = Math.round(k * x + q);
                Color color = lerpColor(c1, c2, (float) i / steps);
                setPixelSafe(x, y, color);
            }
        }
    }

    private void setPixelSafe(int x, int y, Color c) {
        int w = raster.getWidth();
        int h = raster.getHeight();
        if (x >= 0 && x < w && y >= 0 && y < h) {
            raster.setPixel(x, y, c.getRGB());
        }
    }

    private Color lerpColor(Color c1, Color c2, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * t);
        int g = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * t);
        int b = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * t);
        return new Color(r, g, b);
    }
}