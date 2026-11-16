package recipe_library.interface_adapter.browse_saved_recipes;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import entity.Recipe;
import recipe_library.interface_adapter.browse_recipes.RecipeDisplayData;
import recipe_library.interface_adapter.recipe_browser.IngredientText;
import recipe_library.interface_adapter.recipe_browser.RecipeBrowserViewModel;
import recipe_library.use_case.browse_saved_recipes.BrowseSavedRecipesOutputBoundary;
import recipe_library.use_case.browse_saved_recipes.BrowseSavedRecipesOutputData;
import recipe_library.use_case.library.RecipeLibraryRecord;

public class BrowseSavedRecipesPresenter implements BrowseSavedRecipesOutputBoundary {

    private final RecipeBrowserViewModel viewModel;

    public BrowseSavedRecipesPresenter(RecipeBrowserViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(BrowseSavedRecipesOutputData outputData) {
        List<RecipeDisplayData> recipes = new ArrayList<>();
        for (RecipeLibraryRecord record : outputData.getRecipes()) {
            recipes.add(toDisplayData(record));
        }
        String status = recipes.isEmpty()
                ? "Your collection is empty. Search TheMealDB to import a recipe."
                : recipes.size() + " saved recipe" + (recipes.size() == 1 ? "" : "s") + ".";
        publish(recipes, status);
    }

    @Override
    public void prepareFailView(String errorMessage) {
        Runnable update = () -> viewModel.setStatus(errorMessage);
        if (SwingUtilities.isEventDispatchThread()) {
            update.run();
        }
        else {
            SwingUtilities.invokeLater(update);
        }
    }

    private RecipeDisplayData toDisplayData(RecipeLibraryRecord record) {
        Recipe recipe = record.getRecipe();
        return new RecipeDisplayData(
                recipe.getId(),
                recipe.getName(),
                record.getCategory(),
                recipe.getServings(),
                recipe.getImageUrl(),
                IngredientText.lines(recipe.getIngredients()),
                recipe.getInstructions(),
                record.getAttribution(),
                null,
                false);
    }

    private void publish(List<RecipeDisplayData> recipes, String status) {
        Runnable update = () -> viewModel.setRecipes(recipes, status);
        if (SwingUtilities.isEventDispatchThread()) {
            update.run();
        }
        else {
            SwingUtilities.invokeLater(update);
        }
    }
}
