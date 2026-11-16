package use_case.view_nutrition;

/**
 * Input boundary for the View Recipe Nutrition use case.
 */
public interface ViewNutritionInputBoundary {

    /**
     * Calculates the available nutrition for the selected recipe.
     */
    void execute(ViewNutritionInputData inputData);
}
