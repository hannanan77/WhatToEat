package view;

import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;

/**
 * Scroll content that stretches to the viewport width and scrolls vertically
 * only when its content is taller than the viewport.
 */
class EditorialScrollContent extends JPanel implements Scrollable {

    EditorialScrollContent() {
        setBackground(EditorialTheme.BACKGROUND);
        setOpaque(true);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect,
                                           int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect,
                                            int orientation, int direction) {
        return Math.max(visibleRect.height - 16, 16);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getParent() instanceof JViewport
                && getPreferredSize().height <= getParent().getHeight();
    }
}
