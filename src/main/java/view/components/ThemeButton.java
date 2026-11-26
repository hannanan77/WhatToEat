package view.components;

import view.UiTheme;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import java.awt.*;

/** Shared action button: default, hover, pressed, disabled, and keyboard focus states. */
public class ThemeButton extends JButton {
    public enum Variant { PRIMARY, SECONDARY }
    private final Variant variant;

    public ThemeButton(String text, boolean primary) {
        this(text, primary ? Variant.PRIMARY : Variant.SECONDARY);
    }

    public ThemeButton(String text, Variant variant) {
        super(text);
        this.variant = variant;
        setFont(UiTheme.BUTTON_FONT);
        setForeground(variant == Variant.PRIMARY
                ? UiTheme.BUTTON_PRIMARY_TEXT : UiTheme.BUTTON_SECONDARY_TEXT);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(UiTheme.BUTTON_PADDING_Y, UiTheme.BUTTON_PADDING_X,
                UiTheme.BUTTON_PADDING_Y, UiTheme.BUTTON_PADDING_X));
        // Paint labels consistently instead of using OS-specific disabled text effects.
        setUI(new BasicButtonUI() {
            @Override
            protected void paintText(Graphics graphics, AbstractButton button,
                                     Rectangle textRect, String text) {
                graphics.setColor(button.isEnabled()
                        ? (isPrimary() ? UiTheme.BUTTON_PRIMARY_TEXT : button.getForeground())
                        : UiTheme.DISABLED_TEXT);
                FontMetrics metrics = graphics.getFontMetrics();
                BasicGraphicsUtils.drawStringUnderlineCharAt(graphics, text,
                        button.getDisplayedMnemonicIndex(), textRect.x,
                        textRect.y + metrics.getAscent());
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.height = Math.max(UiTheme.BUTTON_MIN_HEIGHT, size.height);
        return size;
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    private boolean isPrimary() {
        return variant == Variant.PRIMARY || isSelected();
    }

    private Color fillColor() {
        if (!isEnabled()) {
            return UiTheme.DISABLED_BACKGROUND;
        }
        boolean primary = isPrimary();
        if (getModel().isPressed() && getModel().isArmed()) {
            return primary ? UiTheme.BUTTON_PRIMARY_PRESSED : UiTheme.BUTTON_SECONDARY_PRESSED;
        }
        if (getModel().isRollover()) {
            return primary ? UiTheme.BUTTON_PRIMARY_HOVER : UiTheme.BUTTON_SECONDARY_HOVER;
        }
        return primary ? UiTheme.BUTTON_PRIMARY_DEFAULT : UiTheme.BUTTON_SECONDARY_DEFAULT;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = UiComponents.smooth(graphics);
        g.setColor(fillColor());
        int arc = UiTheme.CONTROL_RADIUS * 2;
        g.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        if (isFocusOwner() && isEnabled()) {
            g.setColor(isPrimary() ? UiTheme.SURFACE : UiTheme.FOCUS_RING);
            g.setStroke(new BasicStroke(UiTheme.FOCUS_WIDTH));
            g.drawRoundRect(3, 3, getWidth() - 7, getHeight() - 7, arc - 4, arc - 4);
        }
        g.dispose();
        super.paintComponent(graphics);
    }
}
