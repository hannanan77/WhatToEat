package interface_adapter.view_nutrition;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

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
    private final AtomicLong requestSequence = new AtomicLong();

    public ViewNutritionController(ViewNutritionInputBoundary viewNutritionInteractor,
                                   ViewNutritionViewModel viewModel) {
        this.viewNutritionInteractor = Objects.requireNonNull(viewNutritionInteractor);
        this.viewModel = Objects.requireNonNull(viewModel);
    }

    public void execute(Recipe selectedRecipe) {
        final long requestId = requestSequence.incrementAndGet();
        final ViewNutritionInputData inputData =
                new ViewNutritionInputData(requestId, selectedRecipe);
        final String recipeName = selectedRecipe == null
                ? "No recipe selected" : selectedRecipe.getName();
        final Integer servings = selectedRecipe == null
                ? null : selectedRecipe.getServings();
        viewModel.setLoading(requestId, recipeName, servings);

        // USDA requests run away from the Swing event thread.
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                viewNutritionInteractor.execute(inputData);
                return null;
            }
        }.execute();
    }

    /** Invalidates a request that may still be finishing in the background. */
    public void cancelCurrentRequest() {
        final long requestId = requestSequence.incrementAndGet();
        viewModel.invalidateRequest(requestId);
    }
}
