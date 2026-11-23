package view.recipe_library;

import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;

import view.UiTheme;

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
        return UiTheme.SCROLL_INCREMENT;
    }

    @Override
    public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect,
                                            int orientation, int direction) {
        return Math.max(visibleRect.height - UiTheme.SCROLL_INCREMENT, UiTheme.SCROLL_INCREMENT);
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
