
package entity;

import java.util.Locale;
import java.util.Map;

/**
 * Represents an ingredient used in a recipe.
 * Each ingredient has a name, a quantity, and a unit.
 * If the quantity is unknown, it is stored as null.
 */
public class Ingredient {

    // Conversion factors for supported weight units.
    // Each factor converts one unit into grams.
    private static final Map<String, Double> WEIGHT_CONVERSIONS = Map.of(
            "g", 1.0,
            "kg", 1000.0,
            "mg", 0.001
    );

    private final String name;
    private final Double quantity;
    private final String unit;

    /**
     * Creates an ingredient with the given information.
     * The name and unit cannot be null or blank.
     * A known quantity must be positive and finite.
     * A null quantity means the amount is unknown.
     * Invalid information causes an IllegalArgumentException.
     */
    public Ingredient(String name, Double quantity, String unit) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Ingredient name must not be null or blank.");
        }

        if (unit == null || unit.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Ingredient unit must not be null or blank.");
        }

        if (quantity != null
                && (!Double.isFinite(quantity) || quantity <= 0.0)) {
            throw new IllegalArgumentException(
                    "Known ingredient quantity must be positive and finite.");
        }

        this.name = name.trim();
        this.quantity = quantity;
        this.unit = unit.trim();
    }

    public String getName() {
        return name;
    }

    public Double getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    /**
     * Checks whether the ingredient has a known quantity.
     * Returns true if the quantity is known.
     * Returns false if the quantity is null.
     */
    public boolean hasKnownQuantity() {
        return quantity != null;
    }

    /**
     * Converts the ingredient's quantity into grams.
     * Supported units are g, kg, and mg.
     * Returns null if the quantity is unknown or
     * the unit cannot be directly converted into grams.
     * For example, 2 kg becomes 2000 g.
     */
    public Double getWeightInGrams() {
        if (!hasKnownQuantity()) {
            return null;
        }

        Double factor = WEIGHT_CONVERSIONS.get(
                unit.toLowerCase(Locale.ROOT)
        );

        if (factor == null) {
            return null;
        }

        return quantity * factor;
    }
}
