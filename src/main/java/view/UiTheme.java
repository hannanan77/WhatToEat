package view;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;

/** Shared design tokens. Components apply these values; Views decide page layout. */
public final class UiTheme {
    // Existing names and values are retained for teammates' current pages.
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

    // Darker action green keeps small white button labels legible.
    public static final Color BUTTON_PRIMARY_DEFAULT = SECONDARY;
    public static final Color BUTTON_PRIMARY_HOVER = Color.decode("#2D5039");
    public static final Color BUTTON_PRIMARY_PRESSED = Color.decode("#24412E");
    public static final Color BUTTON_PRIMARY_TEXT = SURFACE;
    public static final Color BUTTON_SECONDARY_DEFAULT = MINT;
    public static final Color BUTTON_SECONDARY_HOVER = Color.decode("#D5E6D9");
    public static final Color BUTTON_SECONDARY_PRESSED = Color.decode("#C3DAC9");
    public static final Color BUTTON_SECONDARY_TEXT = SECONDARY;
    public static final Color DISABLED_BACKGROUND = BORDER;
    public static final Color DISABLED_TEXT = Color.decode("#66736A");
    public static final Color FOCUS_RING = SECONDARY;
    public static final Color SCROLL_TRACK = MINT;
    public static final Color SCROLL_THUMB = PRIMARY;
    public static final Color SCROLL_THUMB_ACTIVE = SECONDARY;
    public static final Color SHADOW_COLOR = new Color(38, 51, 43, 18);

    public static final String FONT_FAMILY = resolveFontFamily();
    public static final Font HEADING_FONT = new Font(FONT_FAMILY, Font.BOLD, 24);
    public static final Font SECTION_HEADING_FONT = new Font(FONT_FAMILY, Font.BOLD, 16);
    public static final Font BODY_LARGE_FONT = new Font(FONT_FAMILY, Font.PLAIN, 15);
    public static final Font BODY_FONT = new Font(FONT_FAMILY, Font.PLAIN, 14);
    public static final Font LABEL_FONT = new Font(FONT_FAMILY, Font.BOLD, 13);
    public static final Font SMALL_LABEL_FONT = new Font(FONT_FAMILY, Font.BOLD, 11);
    public static final Font CAPTION_FONT = new Font(FONT_FAMILY, Font.PLAIN, 12);
    public static final Font BUTTON_FONT = LABEL_FONT;
    // Styled paragraphs use 40% extra line spacing. Input fields use font metrics.
    public static final float PARAGRAPH_EXTRA_LINE_SPACING = 0.40f;
    public static final int READING_MAX_WIDTH = 680;

    public static final int SPACE_XS = 4;
    public static final int SPACE_SM = 8;
    public static final int SPACE_MD = 12;
    public static final int SPACE_LG = 16;
    public static final int SPACE_XL = 24;
    public static final int PAGE_PADDING = SPACE_XL;
    public static final int CARD_PADDING = 20;
    public static final int SECTION_GAP = SPACE_XL;
    public static final int FIELD_GAP = SPACE_LG;
    public static final int LABEL_GAP = SPACE_SM;

    // All dimensions are Swing logical pixels, not physical screen pixels.
    public static final int CONTROL_MIN_HEIGHT = 44;
    public static final int BUTTON_MIN_HEIGHT = 44;
    public static final int BUTTON_PADDING_X = 16;
    public static final int BUTTON_PADDING_Y = 12;
    public static final int INPUT_PADDING_X = 12;
    public static final int INPUT_PADDING_Y = 8;
    public static final int CONTROL_RADIUS = 12;
    public static final int CARD_RADIUS = 18;
    public static final int DIALOG_RADIUS = 24;
    public static final int PILL_RADIUS = 100;
    public static final int FOCUS_WIDTH = 2;
    public static final int ARROW_WIDTH = 28;
    public static final int ARROW_HEIGHT = 18;
    public static final int SCROLLBAR_WIDTH = 12;
    public static final int SCROLL_THUMB_MIN_LENGTH = 36;
    public static final int SCROLL_INCREMENT = 18;
    public static final int TABLE_ROW_MIN_HEIGHT = 44;
    public static final int TABLE_HEADER_MIN_HEIGHT = 38;
    public static final int SHADOW_SIZE = 6;
    public static final int SHADOW_OFFSET_Y = 2;

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
