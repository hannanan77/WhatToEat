package interface_adapter.recipe_browser;

import java.util.ArrayList;
import java.util.List;

import entity.Ingredient;

public final class IngredientText {

    private IngredientText() {
    }

    public static List<String> lines(List<Ingredient> ingredients) {
        List<String> lines = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            if (ingredient.getQuantity() == null) {
                lines.add(ingredient.getName() + " (" + ingredient.getUnit() + ")");
            }
            else {
                lines.add(formatQuantity(ingredient.getQuantity())
                        + " " + ingredient.getUnit() + " " + ingredient.getName());
            }
        }
        return lines;
    }

    private static String formatQuantity(double quantity) {
        if (quantity == Math.rint(quantity)) {
            return Long.toString(Math.round(quantity));
        }
        return Double.toString(quantity);
    }
}
