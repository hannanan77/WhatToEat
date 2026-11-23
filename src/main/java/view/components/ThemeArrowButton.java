package view.components;

import view.UiTheme;
import javax.swing.*;
import java.awt.*;

public class ThemeArrowButton extends JButton {
    private final boolean up;
    public ThemeArrowButton(boolean up) {
        this.up = up;
        setPreferredSize(new Dimension(UiTheme.ARROW_WIDTH, UiTheme.ARROW_HEIGHT));
        setMinimumSize(new Dimension(20, 14));
        setBorder(BorderFactory.createEmptyBorder());
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusable(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        getAccessibleContext().setAccessibleName(up ? "Increase value" : "Decrease value");
    }
    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = UiComponents.smooth(graphics);
        boolean active = getModel().isPressed() || getModel().isRollover();
        g.setColor(active ? UiTheme.PRIMARY : UiTheme.MINT);
        g.fillRoundRect(1, 1, Math.max(0, getWidth() - 2), Math.max(0, getHeight() - 2),
                UiTheme.CONTROL_RADIUS, UiTheme.CONTROL_RADIUS);
        g.setColor(!isEnabled() ? UiTheme.TEXT_SECONDARY
                : active ? UiTheme.SURFACE : UiTheme.SECONDARY);
        int x = getWidth() / 2;
        int y = getHeight() / 2;
        int sign = up ? -1 : 1;
        g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(x - 4, y - sign * 2, x, y + sign * 2);
        g.drawLine(x, y + sign * 2, x + 4, y - sign * 2);
        g.dispose();
    }
}
