package controller;

import clip.Clipper;
import fill.ScanLine;
import fill.SeedFill;
import model.Line;
import model.Point;
import model.Polygon;
import rasterize.LineRasterizer;
import rasterize.LineRasterizerAA;
import rasterize.LineRasterizerTrivial;
import rasterize.Raster;
import view.Panel;
import view.SettingsPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Controller2D {
    private final Panel panel;
    private LineRasterizer lineRasterizer;
    private final LineRasterizer rasterizerAA;
    private final LineRasterizer rasterizerTrivial;
    private boolean aaEnabled = false;

    private final ArrayList<Line> lines = new ArrayList<>();
    private final ArrayList<Polygon> polygons = new ArrayList<>();

    private Point tempPoint;
    private Line tempLine;
    private Polygon tempPolygon = new Polygon();
    private Line tempPolygonLine;

    private Polygon tempRectForDrawing = new Polygon();

    private Polygon clippingPolygon = new Polygon();
    private Polygon subjectPolygon = new Polygon();
    private final ArrayList<Polygon> clippedPolygons = new ArrayList<>();

    private Color fillColor = Color.WHITE;
    private Color borderColor = Color.BLUE;

    private Color currentC1 = Color.WHITE;
    private Color currentC2 = Color.WHITE;
    private static final int PICK_RADIUS = 30;

    private enum Mode {LINES, POLYGON, RECT, FILL, CLIP}

    private Mode currentMode = Mode.LINES;

    private boolean shiftPressed = false;
    private boolean rmbDragging = false;

    private Line selectedLine = null;
    private boolean selectedLineIsStart = false;
    private int selectedLineIndex = -1;
    private Polygon selectedPolygon = null;
    private int selectedPolygonIndex = -1;

    private final SettingsPanel settingsPanel;

    private enum FillMode {
        SCANLINE, SEED_BG, SEED_BORDER
    }

    private static class ScanLineData {
        Polygon polygon;
        int fillColor;
        boolean usePattern;

        public ScanLineData(Polygon polygon, int fillColor, boolean usePattern) {
            this.polygon = polygon;
            this.fillColor = fillColor;
            this.usePattern = usePattern;
        }
    }

    private static class SeedFillData {
        Point startPoint;
        FillMode mode;
        int fillColor;
        int borderColor;
        boolean usePattern;

        public SeedFillData(Point startPoint, FillMode mode, int fillColor, int borderColor, boolean usePattern) {
            this.startPoint = startPoint;
            this.mode = mode;
            this.fillColor = fillColor;
            this.borderColor = borderColor;
            this.usePattern = usePattern;
        }
    }

    private final ArrayList<ScanLineData> filledPolygons = new ArrayList<>();
    private final ArrayList<SeedFillData> seedFillDataList = new ArrayList<>();
    private FillMode fillMode = FillMode.SCANLINE;

    public Controller2D(Panel panel) {
        this.panel = panel;
        this.rasterizerAA = new LineRasterizerAA(panel.getRaster());
        this.rasterizerTrivial = new LineRasterizerTrivial(panel.getRaster());

        this.lineRasterizer = rasterizerTrivial;

        settingsPanel = new SettingsPanel(e -> applySettings());
        initListeners();
        drawScene();
    }

    private void applySettings() {
        boolean userAaSetting = settingsPanel.isAAEnabled();

        currentC1 = settingsPanel.getFirstColor();
        currentC2 = settingsPanel.getSecondColor();
        fillColor = settingsPanel.getFillColor();
        borderColor = settingsPanel.getBorderColor();

        String selectedMode = settingsPanel.getFillMode();
        fillMode = switch (selectedMode) {
            case "Seed (Bg)" -> FillMode.SEED_BG;
            case "Seed (Border)" -> FillMode.SEED_BORDER;
            default -> FillMode.SCANLINE;
        };

        if (fillMode == FillMode.SEED_BORDER) {
            aaEnabled = false;
        } else {
            aaEnabled = userAaSetting;
        }

        lineRasterizer = aaEnabled ? rasterizerAA : rasterizerTrivial;

        drawScene();
    }

    private List<Point> calculateRectangle(Point p1, Point p2, Point p3) {
        double v12x = p2.getX() - p1.getX();
        double v12y = p2.getY() - p1.getY();

        double v13x = p3.getX() - p1.getX();
        double v13y = p3.getY() - p1.getY();

        double dotProduct = v13x * v12x + v13y * v12y;
        double v12LengthSq = v12x * v12x + v12y * v12y;

        if (v12LengthSq == 0) return null;

        double scalarProjection = dotProduct / v12LengthSq;

        double vpx = scalarProjection * v12x;
        double vpy = scalarProjection * v12y;

        double vhx = v13x - vpx;
        double vhy = v13y - vpy;

        Point p4 = new Point((int) Math.round(p1.getX() + vhx), (int) Math.round(p1.getY() + vhy));
        Point p3Final = new Point((int) Math.round(p2.getX() + vhx), (int) Math.round(p2.getY() + vhy));

        List<Point> rectPoints = new ArrayList<>();
        rectPoints.add(p1);
        rectPoints.add(p2);
        rectPoints.add(p3Final);
        rectPoints.add(p4);

        return rectPoints;
    }

    private boolean getClosestPoint(Point mouse) {
        int best = PICK_RADIUS;

        selectedLine = null;
        selectedLineIndex = -1;
        selectedLineIsStart = false;

        selectedPolygon = null;
        selectedPolygonIndex = -1;

        for (int i = 0; i < lines.size(); i++) {
            Line L = lines.get(i);

            int d1 = calculateDistance(mouse, L.p1());
            if (d1 < best) {
                best = d1;
                selectedLine = L;
                selectedLineIndex = i;
                selectedLineIsStart = true;
            }

            int d2 = calculateDistance(mouse, L.p2());
            if (d2 < best) {
                best = d2;
                selectedLine = L;
                selectedLineIndex = i;
                selectedLineIsStart = false;
            }
        }

        for (Polygon poly : polygons) {
            var pts = poly.points();
            for (int i = 0; i < pts.size(); i++) {
                int d = calculateDistance(mouse, pts.get(i));
                if (d < best) {
                    best = d;
                    selectedPolygon = poly;
                    selectedPolygonIndex = i;
                }
            }
        }

        return selectedLine != null || selectedPolygon != null;
    }

    public Polygon getClosestPolygon(Point mouse) {
        if (polygons.isEmpty()) {
            return null;
        }

        Polygon result = null;
        int bestDist = Integer.MAX_VALUE;

        for (Polygon polygon : polygons) {
            var pts = polygon.points();
            if (pts.isEmpty()) continue;

            for (Point point : pts) {
                int d = calculateDistance(mouse, point);
                if (d < bestDist) {
                    bestDist = d;
                    result = polygon;
                }
            }
        }

        return result;
    }

    private void deleteClosestPoint(Point mouse) {
        boolean found = getClosestPoint(mouse);
        if (!found) return;

        if (selectedPolygon != null && selectedPolygonIndex >= 0) {
            var pts = selectedPolygon.points();
            if (selectedPolygonIndex < pts.size()) {
                pts.remove(selectedPolygonIndex);
                if (pts.size() < 2) {
                    polygons.remove(selectedPolygon);
                    filledPolygons.removeIf(d -> d.polygon.equals(selectedPolygon));
                }
            }
        } else if (selectedLine != null && selectedLineIndex >= 0) {
            if (selectedLineIndex < lines.size()) {
                lines.remove(selectedLineIndex);
            }
        }

        seedFillDataList.clear();

        selectedLine = null;
        selectedLineIndex = -1;
        selectedLineIsStart = false;
        selectedPolygon = null;
        selectedPolygonIndex = -1;
        drawScene();
    }


    private Point snapAxis(Point from, Point mouse) {
        int dx = mouse.getX() - from.getX();
        int dy = mouse.getY() - from.getY();
        int adx = Math.abs(dx), ady = Math.abs(dy);

        if (adx >= 2 * ady) {
            return new Point(mouse.getX(), from.getY());
        } else if (ady >= 2 * adx) {
            return new Point(from.getX(), mouse.getY());
        } else {
            int sdx = Integer.signum(dx);
            int sdy = Integer.signum(dy);
            int len = Math.max(adx, ady);
            return new Point(from.getX() + sdx * len, from.getY() + sdy * len);
        }
    }

    private int calculateDistance(Point a, Point b) {
        int dx = a.getX() - b.getX();
        int dy = a.getY() - b.getY();
        return (int) Math.sqrt(dx * dx + dy * dy);
    }

    private void performClipping() {
        if (clippingPolygon.points().size() >= 3 && subjectPolygon.points().size() >= 3) {
            Clipper clipper = new Clipper();

            List<Point> resultPoints = clipper.clip(subjectPolygon.points(), clippingPolygon.points());

            if (resultPoints != null && resultPoints.size() >= 3) {
                Polygon clippedPoly = new Polygon();
                for (Point p : resultPoints) {
                    clippedPoly.addPoint(p);
                }
                clippedPolygons.add(clippedPoly);
                filledPolygons.add(new ScanLineData(clippedPoly, fillColor.getRGB(), settingsPanel.isPatternEnabled()));
            }

            subjectPolygon = new Polygon();
        }
    }

    private void initListeners() {
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (currentMode == Mode.CLIP) {
                    Point mouse = new Point(e.getX(), e.getY());
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        tempPoint = mouse;
                        tempPolygon = clippingPolygon;
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        tempPoint = mouse;
                        tempPolygon = subjectPolygon;
                    }
                    return;
                }

                if (SwingUtilities.isRightMouseButton(e)) {
                    Point mouse = new Point(e.getX(), e.getY());

                    if (e.isControlDown()) {
                        deleteClosestPoint(mouse);
                        return;
                    }

                    rmbDragging = getClosestPoint(mouse);
                    return;
                }

                if (currentMode == Mode.LINES || currentMode == Mode.RECT) {
                    tempPoint = new Point(e.getX(), e.getY());
                }

                if (currentMode == Mode.FILL) {
                    Point click = new Point(e.getX(), e.getY());
                    int fillC = fillColor.getRGB();
                    int borderC = borderColor.getRGB();
                    boolean usePattern = settingsPanel.isPatternEnabled();

                    if (fillMode == FillMode.SCANLINE) {
                        Polygon poly = getClosestPolygon(click);
                        if (poly != null) {
                            boolean alreadyFilled = filledPolygons.stream().anyMatch(d -> d.polygon == poly);
                            if (!alreadyFilled) {
                                filledPolygons.add(new ScanLineData(poly, fillC, usePattern));
                            }
                        }
                    } else {
                        seedFillDataList.add(new SeedFillData(click, fillMode, fillC, borderC, usePattern));
                    }
                    drawScene();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) && currentMode != Mode.CLIP) {
                    rmbDragging = false;
                    selectedLine = null;
                    selectedLineIndex = -1;
                    selectedPolygon = null;
                    selectedPolygonIndex = -1;
                    drawScene();
                    return;
                }

                if (currentMode == Mode.CLIP) {
                    if (tempPoint != null) {
                        Point mouse = new Point(e.getX(), e.getY());
                        Point end = shiftPressed ? snapAxis(tempPoint, mouse) : mouse;

                        if (!tempPolygon.points().isEmpty() && tempPolygon.points().get(0).equals(end)) {
                        } else if (!tempPolygon.points().isEmpty() && tempPolygon.getLast().equals(end)) {
                        } else {
                            tempPolygon.addPoint(end);
                        }

                        tempPoint = null;
                        tempLine = null;
                        tempPolygonLine = null;
                        drawScene();
                    }
                    return;
                }

                if (currentMode == Mode.LINES) {
                    if (tempPoint != null) {
                        Point mouse = new Point(e.getX(), e.getY());
                        Point end = shiftPressed ? snapAxis(tempPoint, mouse) : mouse;
                        lines.add(new Line(tempPoint, end, currentC1, currentC2));
                        tempPoint = null;
                        tempLine = null;
                        drawScene();
                    }
                } else if (currentMode == Mode.POLYGON) {
                    Point mouse = new Point(e.getX(), e.getY());
                    if (!tempPolygon.points().isEmpty()) {
                        Point last = tempPolygon.getLast();
                        Point end = shiftPressed ? snapAxis(last, mouse) : mouse;
                        tempPolygon.addPoint(end);
                    } else {
                        tempPolygon.addPoint(mouse);
                    }
                    drawScene();
                } else if (currentMode == Mode.RECT) {
                    Point mouse = new Point(e.getX(), e.getY());

                    if (tempPolygon.points().size() < 2) {
                        Point last = tempPolygon.points().isEmpty() ? mouse : tempPolygon.getLast();
                        Point end = shiftPressed ? snapAxis(last, mouse) : mouse;
                        tempPolygon.addPoint(end);

                        tempRectForDrawing = new Polygon();
                        tempLine = null;
                        drawScene();
                    } else {
                        Point p1 = tempPolygon.getPoint(0);
                        Point p2 = tempPolygon.getPoint(1);
                        Point p3 = shiftPressed ? snapAxis(p2, mouse) : mouse;

                        List<Point> rectPoints = calculateRectangle(p1, p2, p3);

                        if (rectPoints != null) {
                            Polygon finalRect = new Polygon();
                            for (Point p : rectPoints) {
                                finalRect.addPoint(p);
                            }
                            polygons.add(finalRect);
                        }

                        tempPolygon = new Polygon();
                        tempRectForDrawing = new Polygon();
                        tempLine = null;
                        tempPolygonLine = null;
                        drawScene();
                    }
                }
            }
        });

        panel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) && rmbDragging && currentMode != Mode.CLIP) {
                    int x = e.getX();
                    int y = e.getY();

                    if (selectedLine != null && selectedLineIndex >= 0) {
                        Point p1 = selectedLine.p1();
                        Point p2 = selectedLine.p2();
                        Color c1 = selectedLine.c1();
                        Color c2 = selectedLine.c2();

                        if (selectedLineIsStart) p1 = new Point(x, y);
                        else p2 = new Point(x, y);

                        lines.set(selectedLineIndex, new Line(p1, p2, c1, c2));
                        selectedLine = lines.get(selectedLineIndex);
                    }

                    if (selectedPolygon != null && selectedPolygonIndex >= 0) {
                        selectedPolygon.points().set(selectedPolygonIndex, new Point(x, y));
                    }

                    tempLine = null;
                    tempPolygonLine = null;
                    drawScene();
                    return;
                }

                if (currentMode == Mode.CLIP) {
                    tempLine = null;
                    tempPolygonLine = null;
                } else if (currentMode == Mode.LINES && tempPoint != null) {
                    Point mouse = new Point(e.getX(), e.getY());
                    Point end = shiftPressed ? snapAxis(tempPoint, mouse) : mouse;
                    tempLine = new Line(tempPoint, end, currentC1, currentC2);
                } else if (currentMode == Mode.POLYGON && !tempPolygon.points().isEmpty()) {
                    Point last = tempPolygon.getLast();
                    Point first = tempPolygon.getPoint(0);
                    Point mouse = new Point(e.getX(), e.getY());
                    Point end = shiftPressed ? snapAxis(last, mouse) : mouse;
                    tempLine = new Line(last, end, currentC1, currentC2);
                    tempPolygonLine = (tempPolygon.points().size() > 1) ? new Line(end, first, currentC1, currentC2) : null;
                } else if (currentMode == Mode.RECT) {
                    Point mouse = new Point(e.getX(), e.getY());
                    if (tempPolygon.points().size() == 1) {
                        Point p1 = tempPolygon.getPoint(0);
                        Point end = shiftPressed ? snapAxis(p1, mouse) : mouse;
                        tempLine = new Line(p1, end, currentC1, currentC2);
                        tempRectForDrawing = new Polygon();
                    } else if (tempPolygon.points().size() == 2) {
                        Point p1 = tempPolygon.getPoint(0);
                        Point p2 = tempPolygon.getPoint(1);
                        Point p3 = shiftPressed ? snapAxis(p2, mouse) : mouse;

                        List<Point> rectPoints = calculateRectangle(p1, p2, p3);

                        tempRectForDrawing = new Polygon();
                        if (rectPoints != null) {
                            for (Point p : rectPoints) {
                                tempRectForDrawing.addPoint(p);
                            }
                        }
                        tempLine = null;
                    }
                }
                drawScene();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (currentMode == Mode.CLIP) {
                    tempLine = null;
                    tempPolygonLine = null;
                    drawScene();
                } else if (currentMode == Mode.POLYGON && !tempPolygon.points().isEmpty()) {
                    Point last = tempPolygon.getLast();
                    Point first = tempPolygon.getPoint(0);
                    Point mouse = new Point(e.getX(), e.getY());
                    Point end = shiftPressed ? snapAxis(last, mouse) : mouse;
                    tempLine = new Line(last, end, currentC1, currentC2);
                    tempPolygonLine = (tempPolygon.points().size() > 1) ? new Line(end, first, currentC1, currentC2) : null;
                    drawScene();
                } else if (currentMode == Mode.RECT && tempPolygon.points().size() == 1) {
                    Point p1 = tempPolygon.getPoint(0);
                    Point mouse = new Point(e.getX(), e.getY());
                    Point end = shiftPressed ? snapAxis(p1, mouse) : mouse;
                    tempLine = new Line(p1, end, currentC1, currentC2);
                    drawScene();
                } else if (currentMode == Mode.RECT && tempPolygon.points().size() == 2) {
                    Point p1 = tempPolygon.getPoint(0);
                    Point p2 = tempPolygon.getPoint(1);
                    Point mouse = new Point(e.getX(), e.getY());
                    Point p3 = shiftPressed ? snapAxis(p2, mouse) : mouse;

                    List<Point> rectPoints = calculateRectangle(p1, p2, p3);

                    tempRectForDrawing = new Polygon();
                    if (rectPoints != null) {
                        for (Point p : rectPoints) {
                            tempRectForDrawing.addPoint(p);
                        }
                    }
                    tempLine = null;
                    drawScene();
                }
            }
        });

        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_C -> clearScene();
                    case KeyEvent.VK_H -> {
                        settingsPanel.setVisible(!settingsPanel.isVisible());
                    }
                    case KeyEvent.VK_M -> {
                        if (currentMode == Mode.LINES) {
                            currentMode = Mode.POLYGON;
                        } else if (currentMode == Mode.POLYGON) {
                            currentMode = Mode.RECT;
                        } else if (currentMode == Mode.RECT) {
                            currentMode = Mode.FILL;
                        } else if (currentMode == Mode.FILL) {
                            currentMode = Mode.CLIP;
                            clippingPolygon = new Polygon();
                            subjectPolygon = new Polygon();
                            clippedPolygons.clear();
                            filledPolygons.removeIf(d -> clippedPolygons.contains(d.polygon));
                        } else {
                            currentMode = Mode.LINES;
                        }
                        tempPoint = null;
                        tempLine = null;
                        tempPolygonLine = null;
                        tempPolygon = new Polygon();
                        tempRectForDrawing = new Polygon();
                        panel.setCurrentMode(currentMode.toString());
                        drawScene();
                    }
                    case KeyEvent.VK_O -> {
                        if (currentMode == Mode.CLIP && !clippingPolygon.points().isEmpty()) {
                            Collections.reverse(clippingPolygon.points());
                            drawScene();
                        }
                    }
                    case KeyEvent.VK_ENTER -> {
                        if (currentMode == Mode.CLIP) {
                            if (clippingPolygon.points().size() >= 3 && subjectPolygon.points().size() >= 3) {
                                performClipping();
                            } else if (clippingPolygon.points().isEmpty() && subjectPolygon.points().size() >= 3) {
                                clippingPolygon = subjectPolygon;
                                subjectPolygon = new Polygon();

                                Collections.reverse(clippingPolygon.points());

                            } else if (clippingPolygon.points().size() >= 3 && subjectPolygon.points().isEmpty() && tempPolygon == clippingPolygon) {

                                Collections.reverse(clippingPolygon.points());
                            }
                            tempPolygon = new Polygon();
                        } else {
                            if (!tempPolygon.points().isEmpty() && tempPolygon.points().size() >= 3)
                                polygons.add(tempPolygon);
                            tempPolygon = new Polygon();
                            tempRectForDrawing = new Polygon();
                        }
                        tempLine = null;
                        tempPolygonLine = null;
                        drawScene();
                    }
                    case KeyEvent.VK_SHIFT -> {
                        shiftPressed = true;
                        drawScene();
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
                    shiftPressed = false;
                    drawScene();
                }
            }
        });
    }

    private void drawScene() {
        panel.getRaster().clear();
        Raster raster = panel.getRaster();

        Color clipColor = new Color(255, 0, 0, 150);
        Color subColor = new Color(0, 255, 0, 150);

        for (Polygon polygon : polygons) {
            int polySize = polygon.points().size();
            for (int i = 1; i <= polySize; i++) {
                Point a = polygon.getPoint((i - 1) % polySize);
                Point b = polygon.getPoint(i % polySize);
                lineRasterizer.rasterize(new Line(a, b, currentC1, currentC2));
            }
        }

        if (currentMode != Mode.CLIP) {
            int tempPolySize = tempPolygon.points().size();
            for (int i = 1; i < tempPolySize; i++) {
                Point a = tempPolygon.getPoint(i - 1);
                Point b = tempPolygon.getPoint(i);
                lineRasterizer.rasterize(new Line(a, b, currentC1, currentC2));
            }
        }

        if (!tempRectForDrawing.points().isEmpty()) {
            int rectSize = tempRectForDrawing.points().size();
            for (int i = 1; i <= rectSize; i++) {
                Point a = tempRectForDrawing.getPoint((i - 1) % rectSize);
                Point b = tempRectForDrawing.getPoint(i % rectSize);
                lineRasterizer.rasterize(new Line(a, b, currentC1, currentC2));
            }
        }

        if (currentMode != Mode.CLIP) {
            if (tempLine != null) lineRasterizer.rasterize(tempLine);
            if (tempPolygonLine != null) lineRasterizer.rasterize(tempPolygonLine);
        }

        for (Line line : lines) lineRasterizer.rasterize(line);

        if (currentMode == Mode.CLIP) {
            int clipSize = clippingPolygon.points().size();
            for (int i = 1; i <= clipSize; i++) {
                Point a = clippingPolygon.getPoint((i - 1) % clipSize);
                Point b = clippingPolygon.getPoint(i % clipSize);
                lineRasterizer.rasterize(new Line(a, b, clipColor, clipColor));
            }

            int subSize = subjectPolygon.points().size();
            for (int i = 1; i <= subSize; i++) {
                Point a = subjectPolygon.getPoint((i - 1) % subSize);
                Point b = subjectPolygon.getPoint(i % subSize);
                lineRasterizer.rasterize(new Line(a, b, subColor, subColor));
            }
        }

        for (ScanLineData data : filledPolygons) {
            int polyFillC = data.fillColor;
            boolean usePattern = data.usePattern;

            ScanLine scanLine = new ScanLine(raster, lineRasterizer, data.polygon, polyFillC, usePattern);
            scanLine.fill();
        }

        for (SeedFillData data : seedFillDataList) {
            int startX = data.startPoint.getX();
            int startY = data.startPoint.getY();

            int storedFillC = data.fillColor;
            int storedBorderC = data.borderColor;
            boolean usePattern = data.usePattern;

            switch (data.mode) {
                case SEED_BG:
                    SeedFill seedFillBg = new SeedFill(raster, storedFillC, startX, startY, usePattern);
                    seedFillBg.fill();
                    break;
                case SEED_BORDER:
                    SeedFill seedFillBorder = new SeedFill(raster, storedFillC, storedBorderC, startX, startY, usePattern);
                    seedFillBorder.fill();
                    break;
                case SCANLINE:
                    break;
            }
        }

        panel.repaint();
    }

    private void clearScene() {
        panel.getRaster().clear();
        lines.clear();
        polygons.clear();
        tempPolygon = new Polygon();
        tempRectForDrawing = new Polygon();
        tempPoint = null;
        tempLine = null;
        tempPolygonLine = null;
        shiftPressed = false;
        filledPolygons.clear();
        seedFillDataList.clear();

        clippingPolygon = new Polygon();
        subjectPolygon = new Polygon();
        clippedPolygons.clear();

        panel.repaint();
    }
}