package recipe_library.interface_adapter.search_online_recipes;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import entity.Recipe;
import recipe_library.interface_adapter.browse_recipes.RecipeDisplayData;
import recipe_library.interface_adapter.recipe_browser.IngredientText;
import recipe_library.interface_adapter.recipe_browser.RecipeBrowserViewModel;
import recipe_library.use_case.online_recipe.OnlineRecipe;
import recipe_library.use_case.search_online_recipes.OnlineRecipeMatch;
import recipe_library.use_case.search_online_recipes.SearchOnlineRecipesOutputBoundary;
import recipe_library.use_case.search_online_recipes.SearchOnlineRecipesOutputData;

public class SearchOnlineRecipesPresenter implements SearchOnlineRecipesOutputBoundary {

    private final RecipeBrowserViewModel viewModel;

    public SearchOnlineRecipesPresenter(RecipeBrowserViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(SearchOnlineRecipesOutputData outputData) {
        List<RecipeDisplayData> recipes = new ArrayList<>();
        for (OnlineRecipeMatch match : outputData.getMatches()) {
            recipes.add(toDisplayData(match));
        }
        String status = recipes.isEmpty()
                ? "No recipes found for \"" + outputData.getQuery() + "\"."
                : recipes.size() + " online recipe"
                + (recipes.size() == 1 ? "" : "s")
                + " found. Nothing has been saved yet.";
        publish(recipes, status);
    }

    @Override
    public void prepareFailView(String errorMessage) {
        publish(viewModel.getRecipes(), errorMessage);
    }

    private RecipeDisplayData toDisplayData(OnlineRecipeMatch match) {
        OnlineRecipe recipe = match.getRecipe();
        Recipe entity = recipe.toRecipe(recipe.getRemoteImageUrl());
        String attribution = OnlineRecipe.ATTRIBUTION;
        if (match.isAlreadyImported()) {
            attribution = attribution + " Already in your collection.";
        }
        return new RecipeDisplayData(
                entity.getId(),
                entity.getName(),
                recipe.getCategory(),
                null,
                recipe.getRemoteImageUrl(),
                IngredientText.lines(entity.getIngredients()),
                entity.getInstructions(),
                attribution,
                recipe.getMealId(),
                !match.isAlreadyImported());
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
