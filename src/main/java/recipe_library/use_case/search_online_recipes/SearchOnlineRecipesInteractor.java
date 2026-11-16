package recipe_library.use_case.search_online_recipes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import recipe_library.use_case.online_recipe.OnlineRecipe;
import recipe_library.use_case.online_recipe.OnlineRecipeGateway;
import recipe_library.use_case.library.RecipeLibraryDataAccessInterface;

/**
 * Searches TheMealDB and reports whether each result is already imported.
 * It does not save anything.
 */
public class SearchOnlineRecipesInteractor implements SearchOnlineRecipesInputBoundary {

    private final OnlineRecipeGateway gateway;
    private final RecipeLibraryDataAccessInterface library;
    private final SearchOnlineRecipesOutputBoundary presenter;

    public SearchOnlineRecipesInteractor(OnlineRecipeGateway gateway,
                                         RecipeLibraryDataAccessInterface library,
                                         SearchOnlineRecipesOutputBoundary presenter) {
        this.gateway = gateway;
        this.library = library;
        this.presenter = presenter;
    }

    @Override
    public void execute(SearchOnlineRecipesInputData inputData) {
        String query = inputData == null ? null : inputData.getQuery();
        if (query == null || query.isBlank()) {
            presenter.prepareFailView("Enter a recipe name.");
            return;
        }

        String trimmed = query.trim();
        try {
            List<OnlineRecipe> recipes = gateway.searchByName(trimmed);
            List<OnlineRecipeMatch> matches = new ArrayList<>();
            for (OnlineRecipe recipe : recipes) {
                boolean imported = library.existsBySourceId(recipe.getMealId());
                matches.add(new OnlineRecipeMatch(recipe, imported));
            }
            presenter.prepareSuccessView(new SearchOnlineRecipesOutputData(trimmed, matches));
        }
        catch (IOException exception) {
            presenter.prepareFailView(failureMessage(exception));
        }
    }

    public static String failureMessage(IOException exception) {
        String message = exception.getMessage();
        if (message != null && message.contains("could not be read")) {
            return "TheMealDB returned data that could not be read.";
        }
        return "TheMealDB could not be reached. Check your connection and try again.";
    }
}
