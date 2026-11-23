package view;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;

/** Shared presentation constants for the WhatToEat Swing interface. */
public final class UiTheme {

    public static final Color BACKGROUND = Color.decode("#F8FAF6");
    public static final Color SURFACE = Color.decode("#FFFFFF");
    public static final Color PRIMARY = Color.decode("#6E9B76");
    public static final Color SECONDARY = Color.decode("#355E43");
    public static final Color MINT = Color.decode("#E6F1E8");
    public static final Color TEXT_PRIMARY = Color.decode("#26332B");
    public static final Color TEXT_SECONDARY = Color.decode("#77847B");
    public static final Color BORDER = Color.decode("#E1E9E2");
    public static final Color WARNING = Color.decode("#D9822B");
    public static final Color ERROR = Color.decode("#BA1A1A");

    public static final String FONT_FAMILY = resolveFontFamily();
    public static final Font HEADING_FONT = new Font(FONT_FAMILY, Font.BOLD, 24);
    public static final Font SECTION_HEADING_FONT = new Font(FONT_FAMILY, Font.BOLD, 16);
    public static final Font BODY_LARGE_FONT = new Font(FONT_FAMILY, Font.PLAIN, 15);
    public static final Font BODY_FONT = new Font(FONT_FAMILY, Font.PLAIN, 14);
    public static final Font LABEL_FONT = new Font(FONT_FAMILY, Font.BOLD, 13);
    public static final Font SMALL_LABEL_FONT = new Font(FONT_FAMILY, Font.BOLD, 11);

    public static final int SPACE_XS = 4;
    public static final int SPACE_SM = 8;
    public static final int SPACE_MD = 12;
    public static final int SPACE_LG = 16;
    public static final int SPACE_XL = 24;

    public static final int CONTROL_RADIUS = 12;
    public static final int CARD_RADIUS = 18;
    public static final int DIALOG_RADIUS = 24;
    public static final int PILL_RADIUS = 100;

    private UiTheme() {
    }

    private static String resolveFontFamily() {
        for (String family : GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames()) {
            if ("Plus Jakarta Sans".equalsIgnoreCase(family)) {
                return family;
            }
        }
        return Font.SANS_SERIF;
    }
}
