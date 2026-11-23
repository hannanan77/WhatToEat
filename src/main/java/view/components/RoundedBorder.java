package view.components;

import view.UiTheme;
import javax.swing.SwingUtilities;
import javax.swing.border.AbstractBorder;
import java.awt.*;

/** Common input border; focus is detected for both fields and composite controls. */
public class RoundedBorder extends AbstractBorder {
    private final int horizontalPadding;
    private final int verticalPadding;

    public RoundedBorder(int horizontalPadding) {
        this(horizontalPadding, UiTheme.INPUT_PADDING_Y);
    }

    public RoundedBorder(int horizontalPadding, int verticalPadding) {
        this.horizontalPadding = horizontalPadding;
        this.verticalPadding = verticalPadding;
    }

    @Override
    public Insets getBorderInsets(Component component) {
        return new Insets(verticalPadding, horizontalPadding, verticalPadding, horizontalPadding);
    }

    @Override
    public Insets getBorderInsets(Component component, Insets insets) {
        insets.set(verticalPadding, horizontalPadding, verticalPadding, horizontalPadding);
        return insets;
    }

    @Override
    public void paintBorder(Component component, Graphics graphics,
                            int x, int y, int width, int height) {
        Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        boolean focused = focus == component || focus != null
                && component instanceof Container
                && SwingUtilities.isDescendingFrom(focus, component);
        Graphics2D g = UiComponents.smooth(graphics);
        g.setColor(focused ? UiTheme.FOCUS_RING : UiTheme.BORDER);
        g.setStroke(new BasicStroke(focused ? UiTheme.FOCUS_WIDTH : 1));
        g.drawRoundRect(x + 1, y + 1, width - 3, height - 3,
                UiTheme.CONTROL_RADIUS * 2, UiTheme.CONTROL_RADIUS * 2);
        g.dispose();
    }
}
