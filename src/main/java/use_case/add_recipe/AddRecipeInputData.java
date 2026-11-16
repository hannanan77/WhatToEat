package use_case.add_recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the information submitted when adding a recipe.
 */
public class AddRecipeInputData {

    private final String name;
    private final List<IngredientInputData> ingredients;
    private final List<String> instructions;
    private final String imageUrl;
    private final Integer servings;

    public AddRecipeInputData(
            String name,
            List<IngredientInputData> ingredients,
            List<String> instructions,
            String imageUrl,
            Integer servings) {

        this.name = name;
        this.ingredients = ingredients == null
                ? null
                : Collections.unmodifiableList(new ArrayList<>(ingredients));
        this.instructions = instructions == null
                ? null
                : Collections.unmodifiableList(new ArrayList<>(instructions));
        this.imageUrl = imageUrl;
        this.servings = servings;
    }

    public String getName() {
        return name;
    }

    public List<IngredientInputData> getIngredients() {
        return ingredients;
    }

    public List<String> getInstructions() {
        return instructions;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Integer getServings() {
        return servings;
    }

    /**
     * Holds the submitted information for one ingredient.
     */
    public static class IngredientInputData {

        private final String name;
        private final Double quantity;
        private final String unit;

        public IngredientInputData(
                String name, Double quantity, String unit) {
            this.name = name;
            this.quantity = quantity;
            this.unit = unit;
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
    }
}