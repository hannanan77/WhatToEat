package view.components;

import view.UiTheme;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Rounded card. Flat by default; optional soft shadow for elevated content. */
public class ThemeCard extends JPanel {
    private final boolean elevated;

    public ThemeCard(String title, JComponent content) {
        this(title, content, false);
    }

    public ThemeCard(String title, JComponent content, boolean elevated) {
        super(new BorderLayout(0, UiTheme.SPACE_LG));
        this.elevated = elevated;
        setOpaque(false);
        setAlignmentX(LEFT_ALIGNMENT);
        int shadow = elevated ? UiTheme.SHADOW_SIZE : 0;
        int padding = UiTheme.CARD_PADDING + shadow;
        setBorder(new EmptyBorder(padding, padding,
                padding + (elevated ? UiTheme.SHADOW_OFFSET_Y : 0), padding));
        if (title != null && !title.isBlank()) {
            JLabel label = new JLabel(title);
            label.setFont(UiTheme.SECTION_HEADING_FONT);
            label.setForeground(UiTheme.SECONDARY);
            add(label, BorderLayout.NORTH);
        }
        add(content, BorderLayout.CENTER);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = UiComponents.smooth(graphics);
        int shadow = elevated ? UiTheme.SHADOW_SIZE : 0;
        int offset = elevated ? UiTheme.SHADOW_OFFSET_Y : 0;
        int width = getWidth() - shadow * 2 - 1;
        int height = getHeight() - shadow * 2 - offset - 1;
        int arc = UiTheme.CARD_RADIUS * 2;
        if (elevated) {
            Color base = UiTheme.SHADOW_COLOR;
            // Low-alpha concentric silhouettes approximate a soft edge without dependencies.
            g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(),
                    Math.max(1, base.getAlpha() / UiTheme.SHADOW_SIZE)));
            for (int spread = shadow; spread > 0; spread--) {
                g.fillRoundRect(shadow - spread, shadow + offset - spread,
                        width + spread * 2, height + spread * 2,
                        arc + spread * 2, arc + spread * 2);
            }
        }
        g.setColor(UiTheme.SURFACE);
        g.fillRoundRect(shadow, shadow, width, height, arc, arc);
        g.setColor(UiTheme.BORDER);
        g.drawRoundRect(shadow, shadow, width, height, arc, arc);
        g.dispose();
    }
}
