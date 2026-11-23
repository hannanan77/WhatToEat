package view.components;

import view.UiTheme;
import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

public class ThemeScrollBarUI extends BasicScrollBarUI {
    private JButton invisibleButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        button.setFocusable(false);
        return button;
    }
    @Override
    protected JButton createDecreaseButton(int orientation) {
        return invisibleButton();
    }
    @Override
    protected JButton createIncreaseButton(int orientation) {
        return invisibleButton();
    }
    @Override
    protected Dimension getMinimumThumbSize() {
        return scrollbar.getOrientation() == JScrollBar.VERTICAL
                ? new Dimension(UiTheme.SCROLLBAR_WIDTH, UiTheme.SCROLL_THUMB_MIN_LENGTH)
                : new Dimension(UiTheme.SCROLL_THUMB_MIN_LENGTH, UiTheme.SCROLLBAR_WIDTH);
    }
    @Override
    protected void paintTrack(Graphics graphics, JComponent component, Rectangle bounds) {
        Graphics2D g = UiComponents.smooth(graphics);
        g.setColor(UiTheme.SCROLL_TRACK);
        g.fillRoundRect(bounds.x + 2, bounds.y, Math.max(0, bounds.width - 4),
                bounds.height, UiTheme.CONTROL_RADIUS, UiTheme.CONTROL_RADIUS);
        g.dispose();
    }
    @Override
    protected void paintThumb(Graphics graphics, JComponent component, Rectangle bounds) {
        if (bounds.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        Graphics2D g = UiComponents.smooth(graphics);
        g.setColor(isDragging || isThumbRollover() ? UiTheme.SCROLL_THUMB_ACTIVE : UiTheme.SCROLL_THUMB);
        g.fillRoundRect(bounds.x + 2, bounds.y + 1, Math.max(0, bounds.width - 4),
                Math.max(0, bounds.height - 2), UiTheme.CONTROL_RADIUS, UiTheme.CONTROL_RADIUS);
        g.dispose();
    }
}
