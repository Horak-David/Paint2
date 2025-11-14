package view;

import rasterize.RasterBufferedImage;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Panel extends JPanel {

    private final RasterBufferedImage raster;
    private String currentMode = "LINES";

    public Panel(int width, int height) {
        setPreferredSize(new Dimension(width, height));
        raster = new RasterBufferedImage(width, height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(raster.getImage(), 0, 0, null);

        Graphics2D g2d = (Graphics2D) g;

        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(5, getHeight() - 45, 175, 40);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.drawString("Mode: " + currentMode, 10, getHeight() - 10);
        g2d.drawString("Press \"H\" to open settings", 10, getHeight() - 30);
    }

    public void setCurrentMode(String mode) {
        this.currentMode = mode;
        repaint();
    }

    public RasterBufferedImage getRaster() {
        return raster;
    }
}