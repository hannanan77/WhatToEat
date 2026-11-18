package recipe_library.data_access;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts a TheMealDB measure string into a quantity and a unit.
 * Quantities that cannot be parsed stay unknown instead of being invented.
 */
public final class IngredientMeasureParser {

    private static final Pattern MIXED_NUMBER = Pattern.compile(
            "^(\\d+)\\s+(\\d+)\\s*/\\s*(\\d+)\\s*(.*)$");
    private static final Pattern FRACTION = Pattern.compile(
            "^(\\d+)\\s*/\\s*(\\d+)\\s*(.*)$");
    private static final Pattern DECIMAL = Pattern.compile(
            "^(\\d+(?:\\.\\d+)?)\\s*(.*)$");

    private IngredientMeasureParser() {
    }

    public static final class ParsedMeasure {
        private final Double quantity;
        private final String unit;

        ParsedMeasure(Double quantity, String unit) {
            this.quantity = quantity;
            this.unit = unit;
        }

        public Double getQuantity() {
            return quantity;
        }

        public String getUnit() {
            return unit;
        }
    }

    public static ParsedMeasure parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ParsedMeasure(null, "unspecified");
        }

        String text = raw.trim()
                .replace('\u00a0', ' ')
                .replace("½", " 1/2")
                .replace("¼", " 1/4")
                .replace("¾", " 3/4")
                .replaceAll("\\s+", " ")
                .trim();

        Matcher mixed = MIXED_NUMBER.matcher(text);
        if (mixed.matches() && !mixed.group(3).equals("0")) {
            double value = Double.parseDouble(mixed.group(1))
                    + Double.parseDouble(mixed.group(2)) / Double.parseDouble(mixed.group(3));
            return new ParsedMeasure(value, unitOrUnspecified(mixed.group(4)));
        }

        Matcher fraction = FRACTION.matcher(text);
        if (fraction.matches() && !fraction.group(2).equals("0")) {
            double value = Double.parseDouble(fraction.group(1))
                    / Double.parseDouble(fraction.group(2));
            return new ParsedMeasure(value, unitOrUnspecified(fraction.group(3)));
        }

        Matcher decimal = DECIMAL.matcher(text);
        if (decimal.matches()) {
            return new ParsedMeasure(
                    Double.parseDouble(decimal.group(1)),
                    unitOrUnspecified(decimal.group(2)));
        }

        return new ParsedMeasure(null, text);
    }

    private static String unitOrUnspecified(String unit) {
        if (unit == null || unit.isBlank()) {
            return "unspecified";
        }
        return unit.trim();
    }
}
