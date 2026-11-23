package view.recipe_library;

import java.awt.Color;

import view.UiTheme;

/**
 * Recipe library colors and type, mapped onto the shared {@link UiTheme}.
 */
public final class EditorialTheme {

    public static final Color BACKGROUND = UiTheme.BACKGROUND;
    public static final Color CARD = UiTheme.SURFACE;
    public static final Color CARD_HOVER = UiTheme.MINT;
    public static final Color CARD_SELECTED = UiTheme.BUTTON_SECONDARY_HOVER;
    public static final Color CARD_SELECTED_HOVER = UiTheme.BUTTON_SECONDARY_PRESSED;
    public static final Color IMAGE_WELL = UiTheme.MINT;
    public static final Color TEXT = UiTheme.TEXT_PRIMARY;
    public static final Color MUTED = UiTheme.TEXT_SECONDARY;

    /** Swing arc diameter, matching {@link view.components.ThemeCard}. */
    public static final int CORNER_ARC = UiTheme.CARD_RADIUS * 2;

    private EditorialTheme() {
    }
}
