package interface_adapter.view_nutrition;

import javax.swing.SwingWorker;

import entity.Recipe;
import use_case.view_nutrition.ViewNutritionInputBoundary;
import use_case.view_nutrition.ViewNutritionInputData;

/**
 * Controller for requesting nutrition for the selected recipe.
 */
public class ViewNutritionController {

    private final ViewNutritionInputBoundary viewNutritionInteractor;
    private final ViewNutritionViewModel viewModel;

    public ViewNutritionController(ViewNutritionInputBoundary viewNutritionInteractor) {
        this(viewNutritionInteractor, null);
    }

    public ViewNutritionController(ViewNutritionInputBoundary viewNutritionInteractor,
                                   ViewNutritionViewModel viewModel) {
        this.viewNutritionInteractor = viewNutritionInteractor;
        this.viewModel = viewModel;
    }

    public void execute(Recipe selectedRecipe) {
        final ViewNutritionInputData inputData =
                new ViewNutritionInputData(selectedRecipe);
        if (viewModel != null && selectedRecipe != null) {
            viewModel.setLoading(selectedRecipe.getName(), selectedRecipe.getServings());
        }

        // USDA requests run away from the Swing event thread.
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                viewNutritionInteractor.execute(inputData);
                return null;
            }
        }.execute();
    }
}
