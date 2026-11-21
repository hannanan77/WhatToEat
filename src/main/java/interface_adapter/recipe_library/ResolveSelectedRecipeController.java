package interface_adapter.recipe_library;

import java.util.function.Consumer;

import javax.swing.SwingWorker;

import entity.Recipe;
import use_case.recipe_library.ResolveSelectedRecipeInteractor;
import use_case.recipe_library.ResolveSelectedRecipeInteractor.Result;

/**
 * Resolves a selection away from the Swing event thread, then reports it there.
 */
public class ResolveSelectedRecipeController implements RecipeSelectionResolver {

    private final ResolveSelectedRecipeInteractor interactor;

    public ResolveSelectedRecipeController(ResolveSelectedRecipeInteractor interactor) {
        this.interactor = interactor;
    }

    @Override
    public void resolve(String recipeId,
                        String externalMealId,
                        Consumer<Recipe> onSuccess,
                        Consumer<String> onFailure) {
        new SwingWorker<Result, Void>() {
            @Override
            protected Result doInBackground() {
                return interactor.resolve(recipeId, externalMealId);
            }

            @Override
            protected void done() {
                try {
                    Result result = get();
                    if (result.getRecipe() != null) {
                        onSuccess.accept(result.getRecipe());
                        return;
                    }
                    onFailure.accept(result.getError());
                }
                catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    onFailure.accept("The recipe could not be loaded.");
                }
                catch (Exception exception) {
                    onFailure.accept("The recipe could not be loaded.");
                }
            }
        }.execute();
    }
}
