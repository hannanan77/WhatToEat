package view.components;

import view.UiTheme;
import javax.swing.JTextPane;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.Dimension;

/** Read-only, wrapping body copy with shared paragraph spacing. */
public class ThemeParagraph extends JTextPane {
    public ThemeParagraph(String text) {
        setEditable(false);
        setFocusable(false);
        setOpaque(false);
        setBorder(null);
        setMargin(new java.awt.Insets(0, 0, 0, 0));
        setFont(UiTheme.BODY_FONT);
        setForeground(UiTheme.TEXT_SECONDARY);
        setText(text);
        SimpleAttributeSet style = new SimpleAttributeSet();
        StyleConstants.setLineSpacing(style, UiTheme.PARAGRAPH_EXTRA_LINE_SPACING);
        StyleConstants.setSpaceAbove(style, 0);
        StyleConstants.setSpaceBelow(style, UiTheme.SPACE_SM);
        getStyledDocument().setParagraphAttributes(0, getDocument().getLength(), style, false);
    }

    /** Honoured by BoxLayout; other page layouts must apply the width limit themselves. */
    @Override
    public Dimension getMaximumSize() {
        return new Dimension(UiTheme.READING_MAX_WIDTH, Integer.MAX_VALUE);
    }
}
