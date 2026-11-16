package interface_adapter.add_recipe;

import use_case.add_recipe.AddRecipeInputBoundary;
import use_case.add_recipe.AddRecipeInputData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Converts form values into input data and starts the use case.
 */
public class AddRecipeController {

    private final AddRecipeInputBoundary interactor;

    public AddRecipeController(AddRecipeInputBoundary interactor) {
        this.interactor = Objects.requireNonNull(interactor);
    }

    public void execute(
            String name,
            List<IngredientFormData> ingredientRows,
            String instructionsText,
            String imageUrl,
            Integer servings) {

        List<AddRecipeInputData.IngredientInputData> ingredients = null;

        if (ingredientRows != null) {
            ingredients = new ArrayList<>();

            for (IngredientFormData row : ingredientRows) {
                if (row == null) {
                    ingredients.add(null);
                }
                else {
                    ingredients.add(
                            new AddRecipeInputData.IngredientInputData(
                                    row.getName(),
                                    row.getQuantity(),
                                    row.getUnit()
                            )
                    );
                }
            }
        }

        List<String> instructions = new ArrayList<>();

        if (instructionsText != null) {
            for (String line : instructionsText.split("\\R")) {
                String step = line.trim();

                // Empty lines are treated as visual spacing.
                if (!step.isEmpty()) {
                    instructions.add(step);
                }
            }
        }

        String normalizedImageUrl = imageUrl;

        if (normalizedImageUrl != null) {
            normalizedImageUrl = normalizedImageUrl.trim();

            if (normalizedImageUrl.isEmpty()) {
                normalizedImageUrl = null;
            }
        }

        AddRecipeInputData inputData = new AddRecipeInputData(
                name,
                ingredients,
                instructions,
                normalizedImageUrl,
                servings
        );

        interactor.execute(inputData);
    }

    /**
     * Plain form values for one ingredient.
     * This is not an Ingredient entity.
     */
    public static class IngredientFormData {

        private final String name;
        private final Double quantity;
        private final String unit;

        public IngredientFormData(
                String name,
                Double quantity,
                String unit) {
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