package interface_adapter.view_nutrition;

import entity.Recipe;
import use_case.view_nutrition.ViewNutritionInputBoundary;
import use_case.view_nutrition.ViewNutritionInputData;

/**
 * Controller for requesting nutrition for the selected recipe.
 */
public class ViewNutritionController {

    private final ViewNutritionInputBoundary viewNutritionInteractor;

    public ViewNutritionController(ViewNutritionInputBoundary viewNutritionInteractor) {
        this.viewNutritionInteractor = viewNutritionInteractor;
    }

    public void execute(Recipe selectedRecipe) {
        final ViewNutritionInputData inputData =
                new ViewNutritionInputData(selectedRecipe);
        viewNutritionInteractor.execute(inputData);
    }
}
