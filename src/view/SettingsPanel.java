package view;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

public class SettingsPanel extends JFrame {
    private final String[] FILL_MODES = {"ScanLine", "Seed (Bg)", "Seed (Border)"};

    private JCheckBox aaCheckBox;
    private JComboBox<String> fillModeComboBox;
    private JButton firstColorBtn;
    private JButton secondColorBtn;
    private JButton fillColorBtn;
    private JButton borderColorBtn;
    private JCheckBox patternEnabledCheckBox;

    private Color firstColor = Color.WHITE;
    private Color secondColor = Color.WHITE;
    private Color fillColor = Color.WHITE;
    private Color borderColor = Color.BLUE;

    private final ActionListener onSettingsChange;

    public SettingsPanel(ActionListener onSettingsChange) {
        this.onSettingsChange = onSettingsChange;
        initUI();
    }

    private void initUI() {
        setTitle("Settings");
        setSize(300, 420);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        JPanel fillerPanel = new JPanel(new GridBagLayout());
        fillerPanel.setBorder(new TitledBorder("Filler"));
        GridBagConstraints fillerGBC = new GridBagConstraints();
        fillerGBC.insets = new Insets(5, 5, 5, 5);
        fillerGBC.fill = GridBagConstraints.HORIZONTAL;

        int fillerRow = 0;

        fillerGBC.gridx = 0;
        fillerGBC.gridy = fillerRow;
        fillerPanel.add(new JLabel("Fill Mode:"), fillerGBC);

        fillModeComboBox = new JComboBox<>(FILL_MODES);
        fillModeComboBox.setSelectedIndex(0);
        fillModeComboBox.addActionListener(e -> notifyChange());
        fillerGBC.gridx = 1;
        fillerPanel.add(fillModeComboBox, fillerGBC);
        fillerRow++;

        fillerGBC.gridx = 0;
        fillerGBC.gridy = fillerRow;
        fillerPanel.add(new JLabel("Fill Color:"), fillerGBC);

        fillColorBtn = new JButton("■");
        fillColorBtn.setBackground(fillColor);
        fillColorBtn.setOpaque(true);
        fillColorBtn.addActionListener(e -> chooseColor("Fill Color", c -> {
            fillColor = c;
            fillColorBtn.setBackground(c);
            notifyChange();
        }));
        fillerGBC.gridx = 1;
        fillerPanel.add(fillColorBtn, fillerGBC);
        fillerRow++;

        fillerGBC.gridx = 0;
        fillerGBC.gridy = fillerRow;
        fillerPanel.add(new JLabel("Border Color:"), fillerGBC);

        borderColorBtn = new JButton("■");
        borderColorBtn.setBackground(borderColor);
        borderColorBtn.setOpaque(true);
        borderColorBtn.addActionListener(e -> chooseColor("Border Color", c -> {
            borderColor = c;
            borderColorBtn.setBackground(c);
            notifyChange();
        }));
        fillerGBC.gridx = 1;
        fillerPanel.add(borderColorBtn, fillerGBC);
        fillerRow++;

        fillerGBC.gridx = 0;
        fillerGBC.gridy = fillerRow;
        fillerPanel.add(new JLabel("Pattern Enabled:"), fillerGBC);

        patternEnabledCheckBox = new JCheckBox();
        patternEnabledCheckBox.setSelected(false);
        patternEnabledCheckBox.addActionListener(e -> notifyChange());
        fillerGBC.gridx = 1;
        fillerPanel.add(patternEnabledCheckBox, fillerGBC);
        fillerRow++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        mainPanel.add(fillerPanel, gbc);
        row++;

        JPanel linePanel = new JPanel(new GridBagLayout());
        linePanel.setBorder(new TitledBorder("Line"));
        GridBagConstraints lineGBC = new GridBagConstraints();
        lineGBC.insets = new Insets(5, 5, 5, 5);
        lineGBC.fill = GridBagConstraints.HORIZONTAL;

        int lineRow = 0;

        lineGBC.gridx = 0;
        lineGBC.gridy = lineRow;
        linePanel.add(new JLabel("Anti-Aliasing:"), lineGBC);

        aaCheckBox = new JCheckBox();
        aaCheckBox.setSelected(false);
        aaCheckBox.addActionListener(e -> notifyChange());
        lineGBC.gridx = 1;
        linePanel.add(aaCheckBox, lineGBC);
        lineRow++;

        lineGBC.gridx = 0;
        lineGBC.gridy = lineRow;
        linePanel.add(new JLabel("Start Color:"), lineGBC);

        firstColorBtn = new JButton("■");
        firstColorBtn.setBackground(firstColor);
        firstColorBtn.setOpaque(true);
        firstColorBtn.addActionListener(e -> chooseColor("Line Start Color", c -> {
            firstColor = c;
            firstColorBtn.setBackground(c);
            notifyChange();
        }));
        lineGBC.gridx = 1;
        linePanel.add(firstColorBtn, lineGBC);
        lineRow++;

        lineGBC.gridx = 0;
        lineGBC.gridy = lineRow;
        linePanel.add(new JLabel("End Color:"), lineGBC);

        secondColorBtn = new JButton("■");
        secondColorBtn.setBackground(secondColor);
        secondColorBtn.setOpaque(true);
        secondColorBtn.addActionListener(e -> chooseColor("Line End Color", c -> {
            secondColor = c;
            secondColorBtn.setBackground(c);
            notifyChange();
        }));
        lineGBC.gridx = 1;
        linePanel.add(secondColorBtn, lineGBC);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        mainPanel.add(linePanel, gbc);

        add(mainPanel);
    }

    private void chooseColor(String title, Consumer<Color> callback) {
        Color selected = JColorChooser.showDialog(this, title, Color.WHITE);
        if (selected != null) {
            callback.accept(selected);
        }
    }

    private void notifyChange() {
        if (onSettingsChange != null) {
            onSettingsChange.actionPerformed(null);
        }
    }

    public boolean isAAEnabled() { return aaCheckBox.isSelected(); }
    public Color getFirstColor() { return firstColor; }
    public Color getSecondColor() { return secondColor; }
    public Color getFillColor() { return fillColor; }
    public Color getBorderColor() { return borderColor; }
    public String getFillMode() { return (String) fillModeComboBox.getSelectedItem(); }
    public boolean isPatternEnabled() { return patternEnabledCheckBox.isSelected(); }
}