package rasterize;

import java.awt.Color;

// Algoritmus rasterizace: antialiasovaná metoda podle Xiaolin Wu
// Postup algoritmu:
// 1 - Spočítá sklon úsečky k = dy / dx (nebo naopak, když je úsečka strmá)
// 2 - Určí, po které ose bude krokovat buď po x (vodorovné úsečky), nebo po y (svislé/strmé úsečky)
// 3 - Druhou souřadnici počítá z rovnice přímky jako reálné číslo (ideálníX / ideálníY)
// 4 - Rozdělí intenzitu pixelu do dvou sousedních pixelů na vedlejší ose podle desetinné části, a ty vykreslí


public class LineRasterizerAA extends LineRasterizer {
    public LineRasterizerAA(RasterBufferedImage raster) {
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
                float idealX = k * (y - y1) + x1;
                int x = (int) Math.floor(idealX);
                float decimal = idealX - x;
                float weightLeft = 1.0f - decimal;
                float weightRight = decimal;

                Color base = lerpColor(c1, c2, (float) i / (float) steps);

                {
                    int r = Math.min(255, Math.max(0, Math.round(base.getRed() * weightLeft)));
                    int g = Math.min(255, Math.max(0, Math.round(base.getGreen() * weightLeft)));
                    int b = Math.min(255, Math.max(0, Math.round(base.getBlue() * weightLeft)));
                    setPixelSafe(x, y, new Color(r, g, b));
                }

                {
                    int r = Math.min(255, Math.max(0, Math.round(base.getRed() * weightRight)));
                    int g = Math.min(255, Math.max(0, Math.round(base.getGreen() * weightRight)));
                    int b = Math.min(255, Math.max(0, Math.round(base.getBlue() * weightRight)));
                    setPixelSafe(x + 1, y, new Color(r, g, b));
                }
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
                float idealY = k * x + q;
                int y = (int) Math.floor(idealY);
                float decimal = idealY - y;
                float weightTop = 1.0f - decimal;
                float weightBot = decimal;

                Color base = lerpColor(c1, c2, (float) i / (float) steps);

                {
                    int r = Math.min(255, Math.max(0, Math.round(base.getRed() * weightTop)));
                    int g = Math.min(255, Math.max(0, Math.round(base.getGreen() * weightTop)));
                    int b = Math.min(255, Math.max(0, Math.round(base.getBlue() * weightTop)));
                    setPixelSafe(x, y, new Color(r, g, b));
                }

                {
                    int r = Math.min(255, Math.max(0, Math.round(base.getRed() * weightBot)));
                    int g = Math.min(255, Math.max(0, Math.round(base.getGreen() * weightBot)));
                    int b = Math.min(255, Math.max(0, Math.round(base.getBlue() * weightBot)));
                    setPixelSafe(x, y + 1, new Color(r, g, b));
                }
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