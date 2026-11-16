package recipe_library.view;

import java.awt.Color;
import java.awt.Font;

/**
 * Shared colors and type for the luxury editorial recipe screens.
 * Later screens can reuse these values in a team-wide theme.
 */
public final class EditorialTheme {

    public static final Color BACKGROUND = Color.decode("#151C25");
    public static final Color CARD = Color.decode("#202B37");
    public static final Color CARD_HOVER = Color.decode("#2A3847");
    public static final Color CARD_SELECTED = Color.decode("#314155");
    public static final Color CARD_SELECTED_HOVER = Color.decode("#3A4C63");
    public static final Color IMAGE_WELL = Color.decode("#1B2632");
    public static final Color GOLD = Color.decode("#BBA37F");
    public static final Color TEXT = Color.decode("#F6F0E6");
    public static final Color MUTED = Color.decode("#AEBAC6");

    public static final int CORNER_ARC = 16;

    private EditorialTheme() {
    }

    public static Font serif(int style, float size) {
        return new Font(Font.SERIF, style, 1).deriveFont(style, size);
    }

    public static Font sans(int style, float size) {
        return new Font(Font.SANS_SERIF, style, 1).deriveFont(style, size);
    }
}
