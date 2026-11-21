package interface_adapter.recipe_library;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import entity.Recipe;
import use_case.recipe_library.OnlineRecipe;
import use_case.recipe_library.OnlineRecipeMatch;
import use_case.recipe_library.SearchOnlineRecipesOutputBoundary;
import use_case.recipe_library.SearchOnlineRecipesOutputData;

public class SearchOnlineRecipesPresenter implements SearchOnlineRecipesOutputBoundary {

    private final RecipeBrowserViewModel viewModel;
    private final AtomicInteger latestRequest = new AtomicInteger();
    private final ThreadLocal<Integer> requestOnThread = new ThreadLocal<>();

    public SearchOnlineRecipesPresenter(RecipeBrowserViewModel viewModel) {
        this.viewModel = viewModel;
    }

    /**
     * Starts a search. A later call makes an older response obsolete.
     */
    int startRequest() {
        return latestRequest.incrementAndGet();
    }

    /**
     * Runs one search on the worker thread and tags its response.
     */
    void runRequest(int request, Runnable action) {
        requestOnThread.set(request);
        try {
            action.run();
        }
        finally {
            requestOnThread.remove();
        }
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
        Integer request = requestOnThread.get();
        Runnable update = () -> {
            if (request != null && request.intValue() != latestRequest.get()) {
                return;
            }
            viewModel.setRecipes(recipes, status);
        };
        if (SwingUtilities.isEventDispatchThread()) {
            update.run();
        }
        else {
            SwingUtilities.invokeLater(update);
        }
    }
}
