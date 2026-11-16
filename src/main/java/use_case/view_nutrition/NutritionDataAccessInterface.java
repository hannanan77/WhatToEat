package use_case.view_nutrition;

import java.util.Optional;

import entity.Ingredient;

/**
 * Provides ingredient nutrition without depending on a particular API.
 */
public interface NutritionDataAccessInterface {

    /**
     * Finds available per-100-gram nutrition data for an ingredient name.
     */
    Optional<IngredientNutritionData> findNutrition(String ingredientName);

    /**
     * Supplies a reliable total weight estimate when direct conversion is not possible.
     * Returning an empty value keeps the ingredient weight unavailable.
     */
    default Optional<Double> estimateWeightInGrams(Ingredient ingredient) {
        return Optional.empty();
    }
}
