package fill;

import rasterize.Raster;

import java.awt.*;
import java.util.Stack;

public class SeedFill implements Filler {
    private final Raster raster;
    private final int fillColor;
    private final int bgColor;
    private int borderColor;
    private final int startX;
    private final int startY;
    private final boolean useBorder;
    private final boolean usePattern;

    public SeedFill(Raster raster, int fillColor, int startX, int startY, boolean usePattern) {
        this.raster = raster;
        this.fillColor = fillColor;
        this.startX = startX;
        this.startY = startY;
        this.bgColor = raster.getPixel(startX, startY);
        this.useBorder = false;
        this.usePattern = usePattern;
    }

    public SeedFill(Raster raster, int fillColor, int borderColor, int startX, int startY, boolean usePattern) {
        this.raster = raster;
        this.fillColor = fillColor;
        this.borderColor = borderColor;
        this.startX = startX;
        this.startY = startY;
        this.useBorder = true;

        this.bgColor = 0;
        this.usePattern = usePattern;
    }

    @Override
    public void fill() {
        if (useBorder) {
            seedFillByBorder(startX, startY);
        } else {
            seedFillByBg(startX, startY);
        }
    }

    private int getPatternColor(int x, int y) {
        int patternSize = 8;
        int colorA = fillColor;
        int colorB = Color.CYAN.getRGB();

        int tileX = x / patternSize;
        int tileY = y / patternSize;

        if ((tileX + tileY) % 2 == 0) {
            return colorA;
        } else {
            return colorB;
        }
    }

    private void seedFillByBg(int x, int y) {
        Stack<int[]> stack = new Stack<>();
        stack.push(new int[]{x, y});

        while (!stack.isEmpty()) {
            int[] pos = stack.pop();
            int cx = pos[0];
            int cy = pos[1];

            if (isInBounds(cx, cy) && raster.getPixel(cx, cy) == bgColor) {
                int colorToSet = usePattern ? getPatternColor(cx, cy) : fillColor;

                raster.setPixel(cx, cy, colorToSet);
                stack.push(new int[]{cx + 1, cy});
                stack.push(new int[]{cx - 1, cy});
                stack.push(new int[]{cx, cy + 1});
                stack.push(new int[]{cx, cy - 1});
            }
        }
    }

    private void seedFillByBorder(int x, int y) {
        Stack<int[]> stack = new Stack<>();
        stack.push(new int[]{x, y});

        int targetColor = raster.getPixel(x, y);

        while (!stack.isEmpty()) {
            int[] pos = stack.pop();
            int cx = pos[0];
            int cy = pos[1];

            if (isInBounds(cx, cy)) {
                int currentPixelColor = raster.getPixel(cx, cy);

                if (currentPixelColor != borderColor && currentPixelColor == targetColor) {

                    int colorToSet = usePattern ? getPatternColor(cx, cy) : fillColor;

                    raster.setPixel(cx, cy, colorToSet);

                    stack.push(new int[]{cx + 1, cy});
                    stack.push(new int[]{cx - 1, cy});
                    stack.push(new int[]{cx, cy + 1});
                    stack.push(new int[]{cx, cy - 1});
                }
            }
        }
    }

    private boolean isInBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < raster.getWidth() && y < raster.getHeight();
    }
}